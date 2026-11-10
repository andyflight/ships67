using System;
using System.IO;
using System.Linq;
using UnityEngine;

namespace Shipmaster.Editor
{
    /// <summary>
    /// Art budgets and naming rules from ArtBudgets.json — the numbers behind openspec/specs/art-pipeline.
    /// Read from disk rather than through AssetDatabase so it works mid-import.
    /// </summary>
    [Serializable]
    public sealed class ArtBudgets
    {
        public const string JsonPath = "Assets/_Project/Scripts/Editor/ArtBudgets.json";
        public const string ModelsFolder = "Assets/_Project/Art/Models/";
        public const string TexturesFolder = "Assets/_Project/Art/Textures/";

        public int version;
        public string[] textureSuffixes;
        public string[] linearTextureSuffixes;
        public string materialPrefix;
        public string colliderPrefix;
        public string socketPrefix;
        public ArtCategory[] categories;

        public static ArtBudgets Load()
        {
            var json = File.ReadAllText(Path.Combine(Path.GetDirectoryName(Application.dataPath)!, JsonPath));
            return JsonUtility.FromJson<ArtBudgets>(json);
        }

        /// <summary>Category from the asset id prefix (text before the first underscore), or null.</summary>
        public ArtCategory FindCategory(string assetId)
        {
            var prefix = assetId.Split('_')[0];
            return categories.FirstOrDefault(c => c.prefix == prefix);
        }
    }
}
