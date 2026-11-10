using System.Collections.Generic;
using System.Linq;
using NUnit.Framework;
using Shipmaster.Editor;

namespace Shipmaster.Tests
{
    /// <summary>
    /// The art gate: every model and texture under Art/ must pass openspec/specs/art-pipeline.
    /// Run alone with: unity test . --mode EditMode --filter Shipmaster.Tests.ArtAssetsTests
    /// </summary>
    public sealed class ArtAssetsTests
    {
        const string NoAssets = "(no art assets yet)";

        static IEnumerable<string> AssetPaths()
        {
            var paths = ArtAssetValidator.ProjectArtPaths().ToList();
            return paths.Count > 0 ? paths : new[] { NoAssets };
        }

        [TestCaseSource(nameof(AssetPaths))]
        public void Asset_PassesArtPipelineSpec(string path)
        {
            if (path == NoAssets)
                Assert.Pass(NoAssets);

            var report = ArtAssetValidator.ValidateAsset(path, ArtBudgets.Load());

            Assert.IsTrue(report.Ok, report.ToString());
        }
    }
}
