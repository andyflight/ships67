using UnityEngine;

namespace Shipmaster.Views
{
    /// <summary>
    /// Loads Resources/Views/{key}; falls back to <see cref="PlaceholderViews"/> when no prefab exists yet.
    /// Swap for an Addressables-based factory once there is enough art to need it.
    /// </summary>
    public sealed class ResourcesViewFactory : IViewFactory
    {
        const string Folder = "Views/";

        public GameObject Create(string key, Transform parent)
        {
            var prefab = Resources.Load<GameObject>(Folder + key);
            var view = prefab != null ? Object.Instantiate(prefab) : PlaceholderViews.Create(key);

            view.name = $"{key}View";
            view.transform.SetParent(parent, worldPositionStays: false);
            return view;
        }
    }
}
