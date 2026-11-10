using System;

namespace Shipmaster.Services.Save
{
    public interface ISaveService
    {
        /// <returns>Saved data, or a fresh <see cref="PlayerData"/> when nothing is saved yet.</returns>
        PlayerData Load();

        void Save(PlayerData data);
    }

    /// <summary>Everything persisted about the player. Bump <see cref="Version"/> on breaking changes.</summary>
    [Serializable]
    public sealed class PlayerData
    {
        public int Version = 1;
        public int Coins;
        public bool AdsRemoved;
    }
}
