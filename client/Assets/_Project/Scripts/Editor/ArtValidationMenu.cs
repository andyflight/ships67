using System.Linq;
using UnityEditor;
using UnityEngine;

namespace Shipmaster.Editor
{
    /// <summary>Shipmaster/Art/Validate All — the same check the EditMode tests run, on demand.</summary>
    static class ArtValidationMenu
    {
        [MenuItem("Shipmaster/Art/Validate All")]
        static void ValidateAll()
        {
            var results = ArtAssetValidator.ValidateProject(ArtBudgets.Load());
            foreach (var (path, report) in results)
            {
                var asset = AssetDatabase.LoadMainAssetAtPath(path);
                if (!report.Ok)
                    Debug.LogError(report, asset);
                else if (report.Warnings.Count > 0)
                    Debug.LogWarning(report, asset);
            }

            var failed = results.Count(r => !r.report.Ok);
            Debug.Log($"[Art] Validated {results.Count} asset(s): {failed} failed.");
        }
    }
}
