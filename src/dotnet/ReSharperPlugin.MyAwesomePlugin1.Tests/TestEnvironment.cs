using System.Threading;
using JetBrains.Application.BuildScript.Application.Zones;
using JetBrains.ReSharper.Feature.Services;
using JetBrains.ReSharper.Psi.CSharp;
using JetBrains.ReSharper.TestFramework;
using JetBrains.TestFramework;
using JetBrains.TestFramework.Application.Zones;
using NUnit.Framework;

[assembly: Apartment(ApartmentState.STA)]

namespace ReSharperPlugin.MyAwesomePlugin1.Tests
{
    [ZoneDefinition]
    public class MyAwesomePlugin1TestEnvironmentZone : ITestsEnvZone, IRequire<PsiFeatureTestZone>, IRequire<IMyAwesomePlugin1Zone> { }

    [ZoneMarker]
    public class ZoneMarker : IRequire<ICodeEditingZone>, IRequire<ILanguageCSharpZone>, IRequire<MyAwesomePlugin1TestEnvironmentZone> { }

    [SetUpFixture]
    public class MyAwesomePlugin1TestsAssembly : ExtensionTestEnvironmentAssembly<MyAwesomePlugin1TestEnvironmentZone> { }
}
