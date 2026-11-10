using System.IO;
using System.Linq;
using UnityEditor;
using UnityEngine;

namespace Shipmaster.Editor
{
    /// <summary>
    /// Import settings for textures in Art/Textures, chosen by the file-name suffix
    /// (<asset_id>_<Suffix>.png) and capped at the asset category's texture budget.
    /// </summary>
    sealed class ArtTexturePostprocessor : AssetPostprocessor
    {
        const string NormalSuffix = "Normal";

        void OnPreprocessTexture()
        {
            if (!assetPath.StartsWith(ArtBudgets.TexturesFolder))
                return;

            var budgets = ArtBudgets.Load();
            var importer = (TextureImporter)assetImporter;
            var suffix = Path.GetFileNameWithoutExtension(assetPath).Split('_').Last();

            importer.textureType = suffix == NormalSuffix ? TextureImporterType.NormalMap : TextureImporterType.Default;
            importer.sRGBTexture = !budgets.linearTextureSuffixes.Contains(suffix);
            importer.mipmapEnabled = true;

            var category = budgets.FindCategory(Path.GetFileName(Path.GetDirectoryName(assetPath)));
            if (category != null)
                importer.maxTextureSize = category.maxTextureSize;
        }

        void OnPostprocessTexture(Texture2D texture)
        {
            if (!assetPath.StartsWith(ArtBudgets.TexturesFolder))
                return;

            ((TextureImporter)assetImporter).GetSourceTextureWidthAndHeight(out var w, out var h);
            var report = ArtAssetValidator.ValidateTexture(assetPath, w, h, ArtBudgets.Load());
            if (!report.Ok)
                Debug.LogError(report);
        }
    }
}
