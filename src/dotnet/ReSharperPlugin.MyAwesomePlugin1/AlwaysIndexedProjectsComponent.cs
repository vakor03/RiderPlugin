// // C# Backend - Keep the configuration simple
// using System.Collections.Generic;
// using System.Linq;
// using JetBrains.Application.Components;
// using JetBrains.Application.Parts;
// using JetBrains.Application.Settings;
// using JetBrains.Application.Settings.WellKnownRootKeys;
// using JetBrains.ProjectModel;
// using JetBrains.Util;
// using JetBrains.Lifetimes;
// using JetBrains.ProjectModel.DataContext;
//
// // Simple settings storage
// [SettingsKey(typeof(EnvironmentSettings), "Always Indexed Projects")]
// public class AlwaysIndexedProjectsSettings
// {
//     [SettingsEntry(new string[0], "Project names that should always be indexed")]
//     public string[] ProjectNames { get; set; }
// }
//
// // Simple service to manage the projects
// [SolutionComponent(Instantiation.DemandAnyThreadUnsafe)]
// public class AlwaysIndexedProjectsService
// {
//     private readonly ISolution _solution;
//     private readonly ISettingsStore _settingsStore;
//     private readonly ILogger _logger;
//
//     public AlwaysIndexedProjectsService(
//         ISolution solution, 
//         ISettingsStore settingsStore,
//         ILogger logger,
//         Lifetime lifetime)
//     {
//         _solution = solution;
//         _settingsStore = settingsStore;
//         _logger = logger;
//     }
//
//     public IEnumerable<string> GetAlwaysIndexedProjects()
//     {
//         var boundSettings = _settingsStore.BindToContextTransient(ContextRange.Smart(_solution.ToDataContext()));
//         return boundSettings.GetValue((AlwaysIndexedProjectsSettings s) => s.ProjectNames) ?? new string[0];
//     }
//
//     public void SetAlwaysIndexedProjects(IEnumerable<string> projectNames)
//     {
//         var boundSettings = _settingsStore.BindToContextTransient(ContextRange.Smart(_solution.ToDataContext()));
//         boundSettings.SetValue((AlwaysIndexedProjectsSettings s) => s.ProjectNames, projectNames.ToArray());
//         _logger.Info($"Updated always-indexed projects: {string.Join(", ", projectNames)}");
//     }
//
//     public IEnumerable<string> GetAllProjectNames()
//     {
//         return _solution.GetAllProjects().Select(p => p.Name).OrderBy(name => name);
//     }
//
//     public bool IsProjectAlwaysIndexed(string projectName)
//     {
//         return GetAlwaysIndexedProjects().Contains(projectName);
//     }
// }