using UnityEngine;

namespace Shipmaster.Views
{
    /// <summary>Primitive stand-ins for art that doesn't exist yet. Add a case per new view key.</summary>
    public static class PlaceholderViews
    {
        public static GameObject Create(string key) => key switch
        {
            ViewKeys.Ship => Primitive(PrimitiveType.Cube, new Vector3(1f, 0.6f, 3f), new Color(0.75f, 0.3f, 0.2f)),
            ViewKeys.Ocean => Primitive(PrimitiveType.Plane, new Vector3(10f, 1f, 10f), new Color(0.1f, 0.35f, 0.6f)),
            _ => Primitive(PrimitiveType.Sphere, Vector3.one, Color.magenta),
        };

        static GameObject Primitive(PrimitiveType type, Vector3 scale, Color color)
        {
            var go = GameObject.CreatePrimitive(type);
            go.transform.localScale = scale;
            go.GetComponent<Renderer>().material.color = color;
            return go;
        }
    }
}
