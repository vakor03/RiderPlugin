using System.Collections.Generic;
using JetBrains.Application.Settings;
using JetBrains.Application.Settings.WellKnownRootKeys;
using JetBrains.Util;
using Newtonsoft.Json;
using NuGet;

namespace ReSharperPlugin.OptionPages;

[SettingsKey(
    // Discover others through usages of SettingsKeyAttribute
    Parent: typeof(EnvironmentSettings),
    Description: "ReSharper SDK – Sample Settings")]
public class SampleSettings
{
    [SettingsEntry(DefaultValue: "Default text", Description: "Private description")]
    public string String;

    [SettingsEntry(DefaultValue: 42, Description: "Private description")]
    public int Integer;

    [SettingsEntry(DefaultValue: true, Description: "Private description")]
    public bool Boolean;

    [SettingsEntry(DefaultValue: SettingsEnum.Second, Description: "Private description")]
    public SettingsEnum RadioSelection;

    [SettingsEntry(DefaultValue: SettingsEnum.Third, Description: "Private description")]
    public SettingsEnum ComboSelection;

    [SettingsEntry(DefaultValue: default(string), Description: "Private description")]
    public string FolderPath;
    
    [SettingsEntry(DefaultValue: "[]", Description: "List of items as JSON")]
    public string ItemsJson;

    // Helper property to work with the list
    public List<string> Items
    {
        get
        {
            try
            {
                return string.IsNullOrEmpty(ItemsJson) 
                    ? new List<string>() 
                    : JsonConvert.DeserializeObject<List<string>>(ItemsJson) ?? new List<string>();
            }
            catch
            {
                return new List<string>();
            }
        }
        set => ItemsJson = JsonConvert.SerializeObject(value ?? new List<string>());
    }
}

public enum SettingsEnum
{
    First,
    Second,
    Third
}