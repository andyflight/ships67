using UnityEditor;

namespace Shipmaster.Editor
{
    /// <summary>
    /// Import settings for Blender FBX files in Art/Models, applied from code on every import
    /// so nobody has to set them by hand in the Inspector.
    /// </summary>
    sealed class ArtModelPostprocessor : AssetPostprocessor
    {
        const string ModelsFolder = "Assets/_Project/Art/Models/";

        void OnPreprocessModel()
        {
            if (!assetPath.StartsWith(ModelsFolder))
                return;

            var importer = (ModelImporter)assetImporter;
            importer.useFileScale = true;
            importer.bakeAxisConversion = true; // Blender is Z-up, Unity is Y-up: fix rotation in the mesh, not the transform.
            importer.importCameras = false;
            importer.importLights = false;
            importer.importVisibility = false;
        }
    }
}
