using UnityEngine;

namespace Shipmaster.Gameplay
{
    /// <summary>
    /// Ship logic. Lives on the root object; the model is a child view, so it can be
    /// replaced without touching this component.
    /// </summary>
    public sealed class Ship : MonoBehaviour
    {
        const float BobHeight = 0.15f;
        const float BobSpeed = 1.2f;

        void Update()
        {
            var position = transform.position;
            position.y = Mathf.Sin(Time.time * BobSpeed) * BobHeight;
            transform.position = position;
        }
    }
}
