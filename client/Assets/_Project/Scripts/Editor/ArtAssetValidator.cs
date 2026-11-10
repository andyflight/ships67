using System.Collections.Generic;
using System.IO;
using System.Linq;
using System.Text.RegularExpressions;
using UnityEditor;
using UnityEngine;

namespace Shipmaster.Editor
{
    /// <summary>
    /// Checks imported models and textures against openspec/specs/art-pipeline. Used by the import
    /// postprocessors (log on import), the Shipmaster/Art menu and the EditMode tests (the gate).
    /// </summary>
    public static class ArtAssetValidator
    {
        const float PositionEpsilon = 1e-3f;
        const float AngleEpsilonDegrees = 0.1f;
        const float CentreTolerance = 0.05f; // fraction of the longest side
        const float GroundTolerance = 0.01f; // metres
        const string SocketBow = "socket_bow";

        public static ArtValidationReport ValidateModel(GameObject root, string assetId, string assetPath, ArtBudgets budgets)
        {
            var report = new ArtValidationReport(assetId);
            var category = budgets.FindCategory(assetId);
            if (category == null)
            {
                report.Error($"Unknown category prefix. Known: {string.Join(", ", budgets.categories.Select(c => c.prefix))}.");
                return report;
            }

            if (!Regex.IsMatch(assetId, category.idPattern))
                report.Error($"Asset id must match {category.idPattern}.");

            if (assetPath != null)
            {
                var expectedPath = $"{ArtBudgets.ModelsFolder}{category.unityFolder}/{assetId}.fbx";
                if (assetPath != expectedPath)
                    report.Error($"Model must be at {expectedPath}.");
            }

            var all = root.GetComponentsInChildren<Transform>(true);
            var colliders = all.Where(t => t.name.StartsWith(budgets.colliderPrefix)).ToList();
            var sockets = all.Where(t => t.name.StartsWith(budgets.socketPrefix)).ToList();
            var renderMeshes = all.Where(t => !colliders.Contains(t) && GetMesh(t) != null).ToList();

            CheckLods(report, assetId, category, renderMeshes);
            CheckTransforms(report, root.transform, renderMeshes.Concat(colliders));
            CheckSockets(report, budgets, category, sockets);
            CheckMaterials(report, budgets, category, renderMeshes);
            CheckBoundsAndOrigin(report, root.transform, category, renderMeshes, sockets);
            return report;
        }

        public static ArtValidationReport ValidateTexture(string assetPath, int width, int height, ArtBudgets budgets)
        {
            var fileName = Path.GetFileNameWithoutExtension(assetPath);
            var folderId = Path.GetFileName(Path.GetDirectoryName(assetPath));
            var report = new ArtValidationReport(fileName);

            var suffix = fileName.StartsWith(folderId + "_") ? fileName.Substring(folderId.Length + 1) : null;
            if (Path.GetExtension(assetPath) != ".png" || suffix == null || !budgets.textureSuffixes.Contains(suffix))
                report.Error($"Texture must be {ArtBudgets.TexturesFolder}<asset_id>/<asset_id>_<{string.Join("|", budgets.textureSuffixes)}>.png.");

            if (!IsPowerOfTwo(width) || !IsPowerOfTwo(height))
                report.Error($"Texture is {width}x{height}; both sides must be powers of two.");

            var category = budgets.FindCategory(folderId);
            if (category == null)
                report.Error($"Folder '{folderId}' is not an asset id with a known category.");
            else if (Mathf.Max(width, height) > category.maxTextureSize)
                report.Error($"Texture is {Mathf.Max(width, height)}px; the {category.prefix} budget is {category.maxTextureSize}px.");
            return report;
        }

        /// <summary>Validates one imported model or texture by its project-relative path.</summary>
        public static ArtValidationReport ValidateAsset(string path, ArtBudgets budgets)
        {
            if (AssetImporter.GetAtPath(path) is TextureImporter textureImporter)
            {
                textureImporter.GetSourceTextureWidthAndHeight(out var w, out var h);
                return ValidateTexture(path, w, h, budgets);
            }

            var model = AssetDatabase.LoadAssetAtPath<GameObject>(path);
            return ValidateModel(model, Path.GetFileNameWithoutExtension(path), path, budgets);
        }

        /// <summary>Every model under Art/Models and texture under Art/Textures, sorted.</summary>
        public static IEnumerable<string> ProjectArtPaths() =>
            FindAssets("t:Model", ArtBudgets.ModelsFolder).Concat(FindAssets("t:Texture2D", ArtBudgets.TexturesFolder));

        public static List<(string path, ArtValidationReport report)> ValidateProject(ArtBudgets budgets) =>
            ProjectArtPaths().Select(p => (p, ValidateAsset(p, budgets))).ToList();

        public static IEnumerable<string> FindAssets(string filter, string folder) =>
            AssetDatabase.FindAssets(filter, new[] { folder.TrimEnd('/') })
                .Select(AssetDatabase.GUIDToAssetPath)
                .Distinct()
                .OrderBy(p => p);

        static void CheckLods(ArtValidationReport report, string assetId, ArtCategory category, List<Transform> renderMeshes)
        {
            var budgets = category.lodTriangles;
            var expected = budgets.Length == 1
                ? new[] { assetId }
                : Enumerable.Range(0, budgets.Length).Select(i => $"{assetId}_LOD{i}").ToArray();

            foreach (var t in renderMeshes.Where(t => !expected.Contains(t.name)))
                report.Error($"Render mesh '{t.name}' is unexpected. Expected exactly: {string.Join(", ", expected)}.");

            for (var i = 0; i < expected.Length; i++)
            {
                var t = renderMeshes.FirstOrDefault(m => m.name == expected[i]);
                if (t == null)
                {
                    report.Error($"Missing render mesh '{expected[i]}'.");
                    continue;
                }

                var mesh = GetMesh(t);
                var triangles = CountTriangles(mesh);
                if (triangles > budgets[i])
                    report.Error($"'{t.name}' has {triangles} triangles; budget is {budgets[i]}.");
                if (!mesh.HasVertexAttribute(UnityEngine.Rendering.VertexAttribute.TexCoord0))
                    report.Error($"'{t.name}' has no UV map.");
            }
        }

        static void CheckTransforms(ArtValidationReport report, Transform root, IEnumerable<Transform> meshes)
        {
            foreach (var t in meshes.Where(t => t != root))
            {
                var identity = t.localPosition.sqrMagnitude < PositionEpsilon * PositionEpsilon
                    && Quaternion.Angle(t.localRotation, Quaternion.identity) < AngleEpsilonDegrees
                    && (t.localScale - Vector3.one).sqrMagnitude < PositionEpsilon * PositionEpsilon;
                if (!identity)
                    report.Error($"'{t.name}' has a non-identity transform; apply transforms in Blender and check the FBX axis settings.");
            }
        }

        static void CheckSockets(ArtValidationReport report, ArtBudgets budgets, ArtCategory category, List<Transform> sockets)
        {
            foreach (var s in sockets.Where(s => (s.lossyScale - Vector3.one).sqrMagnitude > PositionEpsilon * PositionEpsilon))
                report.Error($"Socket '{s.name}' must have scale 1.");

            foreach (var required in category.requiredSockets.Where(r => sockets.All(s => s.name != r)))
                report.Error($"Required socket '{required}' is missing.");
        }

        static void CheckMaterials(ArtValidationReport report, ArtBudgets budgets, ArtCategory category, List<Transform> renderMeshes)
        {
            var materials = renderMeshes
                .Select(t => t.GetComponent<Renderer>())
                .Where(r => r != null)
                .SelectMany(r => r.sharedMaterials)
                .ToList();

            if (materials.Any(m => m == null))
                report.Error("A render mesh has an empty material slot.");

            var distinct = materials.Where(m => m != null).Select(m => m.name).Distinct().ToList();
            if (distinct.Count > category.maxMaterials)
                report.Error($"{distinct.Count} materials; the {category.prefix} budget is {category.maxMaterials}.");
            foreach (var name in distinct.Where(n => !n.StartsWith(budgets.materialPrefix)))
                report.Error($"Material '{name}' must start with '{budgets.materialPrefix}'.");
        }

        static void CheckBoundsAndOrigin(ArtValidationReport report, Transform root, ArtCategory category,
            List<Transform> renderMeshes, List<Transform> sockets)
        {
            var lod0 = renderMeshes.FirstOrDefault(t => t.name == root.name || t.name.EndsWith("_LOD0"));
            if (lod0 == null)
                return;

            var bounds = BoundsInRootSpace(root, lod0, GetMesh(lod0));
            var size = bounds.size;
            var longest = Mathf.Max(size.x, size.y, size.z);
            if (longest < category.minSizeMeters || longest > category.maxSizeMeters)
                report.Error($"Longest side is {longest:0.##} m; expected {category.minSizeMeters}–{category.maxSizeMeters} m. Check units and scale.");

            switch (category.origin)
            {
                case ArtCategory.OriginWaterline:
                    if (size.z < size.x)
                        report.Error("Ship length must run along Unity +Z (bow toward -Y in Blender).");
                    var bow = sockets.FirstOrDefault(s => s.name == SocketBow);
                    if (bow != null && root.InverseTransformPoint(bow.position).z <= 0)
                        report.Error($"{SocketBow} must be at positive Z in Unity: the bow faces forward.");
                    if (bounds.min.y >= 0)
                        report.Warn("Nothing below Y=0: the origin should be on the waterline with the hull dipping under it.");
                    if (Mathf.Abs(bounds.center.x) > CentreTolerance * longest)
                        report.Warn("Ship is not centred on X.");
                    break;

                case ArtCategory.OriginBottom:
                    if (Mathf.Abs(bounds.min.y) > GroundTolerance)
                        report.Error($"Bottom of the model is at Y={bounds.min.y:0.###}; it must rest on Y=0.");
                    if (Mathf.Abs(bounds.center.x) > CentreTolerance * longest || Mathf.Abs(bounds.center.z) > CentreTolerance * longest)
                        report.Warn("Model is not centred on X/Z.");
                    break;

                case ArtCategory.OriginMount:
                    // Origin is the mount point; geometry may extend anywhere from it.
                    break;
            }
        }

        static Bounds BoundsInRootSpace(Transform root, Transform t, Mesh mesh)
        {
            var b = mesh.bounds;
            var result = new Bounds(root.InverseTransformPoint(t.TransformPoint(b.center)), Vector3.zero);
            for (var i = 0; i < 8; i++)
            {
                var corner = b.center + Vector3.Scale(b.extents, new Vector3((i & 1) == 0 ? -1 : 1, (i & 2) == 0 ? -1 : 1, (i & 4) == 0 ? -1 : 1));
                result.Encapsulate(root.InverseTransformPoint(t.TransformPoint(corner)));
            }

            return result;
        }

        static Mesh GetMesh(Transform t)
        {
            if (t.TryGetComponent<MeshFilter>(out var filter))
                return filter.sharedMesh;
            return t.TryGetComponent<SkinnedMeshRenderer>(out var skinned) ? skinned.sharedMesh : null;
        }

        static int CountTriangles(Mesh mesh)
        {
            long indices = 0;
            for (var i = 0; i < mesh.subMeshCount; i++)
                indices += mesh.GetIndexCount(i);
            return (int)(indices / 3);
        }

        static bool IsPowerOfTwo(int n) => n > 0 && (n & (n - 1)) == 0;
    }
}
