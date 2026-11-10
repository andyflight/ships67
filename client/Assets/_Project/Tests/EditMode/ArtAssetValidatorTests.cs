using System.Collections.Generic;
using System.Linq;
using NUnit.Framework;
using Shipmaster.Editor;
using UnityEngine;

namespace Shipmaster.Tests
{
    /// <summary>Validator rules on in-memory models, independent of ArtBudgets.json numbers.</summary>
    public sealed class ArtAssetValidatorTests
    {
        const string TexturePath = "Assets/_Project/Art/Textures/prop_barrel/prop_barrel_";

        readonly List<Object> _created = new();
        ArtBudgets _budgets;

        [SetUp]
        public void SetUp()
        {
            _budgets = new ArtBudgets
            {
                textureSuffixes = new[] { "BaseMap", "Normal", "MaskMap", "PaintMask" },
                linearTextureSuffixes = new[] { "Normal", "MaskMap", "PaintMask" },
                materialPrefix = "M_",
                colliderPrefix = "COL_",
                socketPrefix = "socket_",
                categories = new[]
                {
                    Category("ship", "^ship_[a-z0-9]+_t[1-9][0-9]*$", ArtCategory.OriginWaterline, 3, 80, "socket_bow"),
                    Category("prop", "^prop_[a-z0-9]+(_[a-z0-9]+)*$", ArtCategory.OriginBottom, 0.1f, 10),
                },
            };
        }

        [TearDown]
        public void TearDown()
        {
            foreach (var o in _created)
                Object.DestroyImmediate(o);
            _created.Clear();
        }

        [Test]
        public void ValidProp_HasNoErrors()
        {
            var report = Validate(Prop(), "prop_barrel");

            Assert.IsTrue(report.Ok, report.ToString());
        }

        [Test]
        public void UnknownCategory_IsError()
        {
            var report = Validate(Model("rock_big", "rock_big", Vector3.one, new Vector3(0, 0.5f, 0)), "rock_big");

            AssertError(report, "Unknown category");
        }

        [Test]
        public void IdNotMatchingPattern_IsError()
        {
            var report = Validate(Model("ship_sloop", "ship_sloop", new Vector3(4, 3, 12), Vector3.zero), "ship_sloop");

            AssertError(report, "must match");
        }

        [Test]
        public void OverTriangleBudget_IsError()
        {
            _budgets.categories[1].lodTriangles = new[] { 5 };

            AssertError(Validate(Prop(), "prop_barrel"), "triangles");
        }

        [Test]
        public void MissingLodMesh_IsError()
        {
            _budgets.categories[1].lodTriangles = new[] { 100, 50 };

            AssertError(Validate(Prop(), "prop_barrel"), "Missing render mesh 'prop_barrel_LOD1'");
        }

        [Test]
        public void FloatingProp_IsError()
        {
            var report = Validate(Model("prop_barrel", "prop_barrel", Vector3.one, new Vector3(0, 1, 0)), "prop_barrel");

            AssertError(report, "rest on Y=0");
        }

        [Test]
        public void MeshWithUnappliedTransform_IsError()
        {
            var root = Prop();
            root.transform.GetChild(0).localPosition = new Vector3(1, 0, 0);

            AssertError(Validate(root, "prop_barrel"), "non-identity transform");
        }

        [Test]
        public void MaterialWithoutPrefix_IsError()
        {
            var root = Prop();
            root.GetComponentInChildren<Renderer>().sharedMaterial.name = "Wood";

            AssertError(Validate(root, "prop_barrel"), "must start with 'M_'");
        }

        [Test]
        public void TooLarge_IsError()
        {
            var report = Validate(Model("prop_barrel", "prop_barrel", new Vector3(1, 20, 1), new Vector3(0, 10, 0)), "prop_barrel");

            AssertError(report, "Longest side");
        }

        [Test]
        public void ColliderMesh_IsNotCountedAsRenderMesh()
        {
            var root = Prop();
            var collider = Model("COL_prop_barrel", "COL_prop_barrel", Vector3.one, new Vector3(0, 0.5f, 0));
            collider.transform.SetParent(root.transform, false);

            Assert.IsTrue(Validate(root, "prop_barrel").Ok);
        }

        [Test]
        public void ValidShip_HasNoErrors()
        {
            var report = Validate(Ship(bowZ: 6), "ship_sloop_t1");

            Assert.IsTrue(report.Ok, report.ToString());
        }

        [Test]
        public void ShipBowFacingBackward_IsError()
        {
            AssertError(Validate(Ship(bowZ: -6), "ship_sloop_t1"), "socket_bow must be at positive Z");
        }

        [Test]
        public void ShipLengthAlongX_IsError()
        {
            var root = Model("ship_sloop_t1", "ship_sloop_t1", new Vector3(12, 3, 4), new Vector3(0, 0.5f, 0));
            AddSocket(root, "socket_bow", new Vector3(0, 0, 1));

            AssertError(Validate(root, "ship_sloop_t1"), "Ship length must run along Unity +Z");
        }

        [Test]
        public void ShipWithoutBowSocket_IsError()
        {
            var root = Model("ship_sloop_t1", "ship_sloop_t1", new Vector3(4, 3, 12), new Vector3(0, 0.5f, 0));

            AssertError(Validate(root, "ship_sloop_t1"), "Required socket 'socket_bow'");
        }

        [Test]
        public void ModelInWrongFolder_IsError()
        {
            var report = ArtAssetValidator.ValidateModel(Prop(), "prop_barrel", "Assets/_Project/Art/Models/prop_barrel.fbx", _budgets);

            AssertError(report, "Model must be at Assets/_Project/Art/Models/Props/prop_barrel.fbx");
        }

        [TestCase("BaseMap", 512, 512, null)]
        [TestCase("PaintMask", 256, 1024, null)]
        [TestCase("BaseMap", 500, 512, "powers of two")]
        [TestCase("Diffuse", 512, 512, "must be")]
        [TestCase("BaseMap", 2048, 2048, "budget")]
        public void Texture_Rules(string suffix, int width, int height, string expectedError)
        {
            var report = ArtAssetValidator.ValidateTexture($"{TexturePath}{suffix}.png", width, height, _budgets);

            if (expectedError == null)
                Assert.IsTrue(report.Ok, report.ToString());
            else
                AssertError(report, expectedError);
        }

        [Test]
        public void ProjectBudgetsJson_DefinesAllCategories()
        {
            var budgets = ArtBudgets.Load();

            CollectionAssert.AreEquivalent(new[] { "ship", "part", "prop", "env" }, budgets.categories.Select(c => c.prefix));
            Assert.IsTrue(budgets.categories.All(c => c.lodTriangles.Length > 0));
        }

        ArtValidationReport Validate(GameObject root, string assetId) =>
            ArtAssetValidator.ValidateModel(root, assetId, null, _budgets);

        static void AssertError(ArtValidationReport report, string fragment) =>
            Assert.IsTrue(report.Errors.Any(e => e.Contains(fragment)), $"Expected an error containing '{fragment}'.\n{report}");

        static ArtCategory Category(string prefix, string pattern, string origin, float min, float max, params string[] sockets) =>
            new()
            {
                prefix = prefix,
                idPattern = pattern,
                unityFolder = prefix == "ship" ? "Ships" : "Props",
                lodTriangles = new[] { 100 },
                maxMaterials = 1,
                maxTextureSize = 1024,
                minSizeMeters = min,
                maxSizeMeters = max,
                origin = origin,
                requiredSockets = sockets,
            };

        GameObject Prop() => Model("prop_barrel", "prop_barrel", Vector3.one, new Vector3(0, 0.5f, 0));

        GameObject Ship(float bowZ)
        {
            // Hull 12 m long on Z, dipping 1 m below the waterline (origin).
            var root = Model("ship_sloop_t1", "ship_sloop_t1", new Vector3(4, 3, 12), new Vector3(0, 0.5f, 0));
            AddSocket(root, "socket_bow", new Vector3(0, 0, bowZ));
            return root;
        }

        GameObject Model(string rootName, string meshName, Vector3 size, Vector3 centre)
        {
            var root = Track(new GameObject(rootName));
            var child = Track(new GameObject(meshName));
            child.transform.SetParent(root.transform, false);
            child.AddComponent<MeshFilter>().sharedMesh = Track(Box(size, centre));
            var material = Track(new Material(Shader.Find("Universal Render Pipeline/Lit")) { name = "M_Wood" });
            child.AddComponent<MeshRenderer>().sharedMaterial = material;
            return root;
        }

        void AddSocket(GameObject root, string name, Vector3 position)
        {
            var socket = Track(new GameObject(name));
            socket.transform.SetParent(root.transform, false);
            socket.transform.localPosition = position;
        }

        T Track<T>(T o) where T : Object
        {
            _created.Add(o);
            return o;
        }

        static Mesh Box(Vector3 size, Vector3 centre)
        {
            var vertices = new Vector3[8];
            for (var i = 0; i < 8; i++)
                vertices[i] = centre + Vector3.Scale(size * 0.5f, new Vector3((i & 1) == 0 ? -1 : 1, (i & 2) == 0 ? -1 : 1, (i & 4) == 0 ? -1 : 1));

            var mesh = new Mesh
            {
                vertices = vertices,
                uv = vertices.Select(v => new Vector2(v.x, v.z)).ToArray(),
                triangles = new[]
                {
                    0, 2, 1, 1, 2, 3, 4, 5, 6, 5, 7, 6, 0, 1, 4, 1, 5, 4,
                    2, 6, 3, 3, 6, 7, 0, 4, 2, 2, 4, 6, 1, 3, 5, 3, 7, 5,
                },
            };
            mesh.RecalculateBounds();
            return mesh;
        }
    }
}
