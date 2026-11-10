using System.Collections.Generic;
using System.Text;

namespace Shipmaster.Editor
{
    /// <summary>Result of checking one art asset against openspec/specs/art-pipeline.</summary>
    public sealed class ArtValidationReport
    {
        public ArtValidationReport(string assetId)
        {
            AssetId = assetId;
        }

        public string AssetId { get; }
        public List<string> Errors { get; } = new();
        public List<string> Warnings { get; } = new();
        public bool Ok => Errors.Count == 0;

        public void Error(string message) => Errors.Add(message);
        public void Warn(string message) => Warnings.Add(message);

        public override string ToString()
        {
            var sb = new StringBuilder($"[Art] {AssetId}: {(Ok ? "OK" : $"{Errors.Count} error(s)")}");
            foreach (var e in Errors) sb.Append("\n  ERROR: ").Append(e);
            foreach (var w in Warnings) sb.Append("\n  warning: ").Append(w);
            return sb.ToString();
        }
    }
}
