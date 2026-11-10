using UnityEngine;

namespace Shipmaster.Views
{
    /// <summary>
    /// Creates the visual part of a game object by key. Gameplay code never references
    /// models directly, so placeholder art can be replaced by Blender art without code changes.
    /// </summary>
    public interface IViewFactory
    {
        GameObject Create(string key, Transform parent);
    }

    /// <summary>View keys. A prefab at Resources/Views/{key} overrides the placeholder.</summary>
    public static class ViewKeys
    {
        public const string Ship = "Ship";
        public const string Ocean = "Ocean";
    }
}
