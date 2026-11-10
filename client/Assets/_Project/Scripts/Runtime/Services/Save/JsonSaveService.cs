using System.IO;
using UnityEngine;

namespace Shipmaster.Services.Save
{
    /// <summary>Local JSON save. Later the server becomes the source of truth and this acts as a cache.</summary>
    public sealed class JsonSaveService : ISaveService
    {
        readonly string _filePath;

        public JsonSaveService(string filePath) => _filePath = filePath;

        public PlayerData Load()
        {
            if (!File.Exists(_filePath))
                return new PlayerData();

            return JsonUtility.FromJson<PlayerData>(File.ReadAllText(_filePath)) ?? new PlayerData();
        }

        public void Save(PlayerData data)
        {
            // Write to a temp file first so a crash mid-write can't corrupt the save.
            var tempPath = _filePath + ".tmp";
            File.WriteAllText(tempPath, JsonUtility.ToJson(data));
            File.Copy(tempPath, _filePath, overwrite: true);
            File.Delete(tempPath);
        }
    }
}
