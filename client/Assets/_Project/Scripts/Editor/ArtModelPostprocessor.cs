using System.IO;
using UnityEditor;
using UnityEngine;

namespace Shipmaster.Editor
{
    /// <summary>
    /// Import settings for Blender FBX files in Art/Models, applied from code on every import
    /// so nobody has to set them by hand in the Inspector. Also turns COL_ meshes into colliders
    /// and logs art-pipeline violations (the EditMode tests are the hard gate).
    /// </summary>
    sealed class ArtModelPostprocessor : AssetPostprocessor
    {
        void OnPreprocessModel()
        {
            if (!assetPath.StartsWith(ArtBudgets.ModelsFolder))
                return;

            var importer = (ModelImporter)assetImporter;
            importer.useFileScale = true;
            importer.bakeAxisConversion = true; // Blender is Z-up, Unity is Y-up: fix rotation in the mesh, not the transform.
            importer.importCameras = false;
            importer.importLights = false;
            importer.importVisibility = false;
        }

        void OnPostprocessModel(GameObject root)
        {
            if (!assetPath.StartsWith(ArtBudgets.ModelsFolder))
                return;

            var budgets = ArtBudgets.Load();
            ConvertColliders(root.transform, budgets.colliderPrefix);

            var report = ArtAssetValidator.ValidateModel(root, Path.GetFileNameWithoutExtension(assetPath), assetPath, budgets);
            if (!report.Ok)
                Debug.LogError(report);
            else if (report.Warnings.Count > 0)
                Debug.LogWarning(report);
        }

        static void ConvertColliders(Transform root, string prefix)
        {
            foreach (var t in root.GetComponentsInChildren<Transform>(true))
            {
                if (!t.name.StartsWith(prefix) || !t.TryGetComponent<MeshFilter>(out var filter))
                    continue;

                var collider = t.gameObject.AddComponent<MeshCollider>();
                collider.sharedMesh = filter.sharedMesh;
                collider.convex = true; // required for colliders on moving rigidbodies (ships)
                Object.DestroyImmediate(t.GetComponent<MeshRenderer>());
                Object.DestroyImmediate(filter);
            }
        }
    }
}
