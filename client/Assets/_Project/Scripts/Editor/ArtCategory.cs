using System;

namespace Shipmaster.Editor
{
    /// <summary>Per-category budget from ArtBudgets.json (ship, part, prop, env).</summary>
    [Serializable]
    public sealed class ArtCategory
    {
        public const string OriginWaterline = "waterline";
        public const string OriginBottom = "bottom";
        public const string OriginMount = "mount";

        public string prefix;
        public string idPattern;
        public string unityFolder;
        public int[] lodTriangles;
        public int maxMaterials;
        public int maxTextureSize;
        public float minSizeMeters;
        public float maxSizeMeters;
        public string origin;
        public string[] requiredSockets;
    }
}
