using System;
using System.Linq;
using JetBrains.Application.Progress;
using JetBrains.ProjectModel;
using JetBrains.ReSharper.Feature.Services.ContextActions;
using JetBrains.ReSharper.Feature.Services.CSharp.ContextActions;
using JetBrains.ReSharper.Psi;
using JetBrains.ReSharper.Psi.CSharp;
using JetBrains.ReSharper.Psi.CSharp.Tree;
using JetBrains.ReSharper.Psi.Util;
using JetBrains.TextControl;
using JetBrains.Util;

[ContextAction(
    Group = CSharpContextActions.GroupID,
    Name = nameof(InjectDependencyContextAction),
    Description = "Inject dependency for MonoBehaviour field",
    Priority = 10)]
public class InjectDependencyContextAction(ICSharpContextActionDataProvider provider) : ContextActionBase {
    public override string Text => "Inject Dependency via [Inject]";

    public override bool IsAvailable(IUserDataHolder cache) {
        IFieldDeclaration fieldDeclaration = provider.GetSelectedElement<IFieldDeclaration>();
        if (fieldDeclaration == null)
            return false;

        if (!IsPrivateField(fieldDeclaration))
            return false;

        IClassDeclaration containingClass = fieldDeclaration.GetContainingTypeDeclaration() as IClassDeclaration;
        if (containingClass == null)
            return false;

        if (!InheritsFromMonoBehaviour(containingClass))
            return false;

        string fieldName = fieldDeclaration.DeclaredName;
        string parameterName = GetParameterName(fieldName);
    
        IMethodDeclaration injectMethod = FindInjectDependenciesMethod(containingClass);
        if (injectMethod != null && IsFieldAlreadyInjected(injectMethod, parameterName, fieldName))
            return false;

        return true;
    }

    private bool IsFieldAlreadyInjected(IMethodDeclaration injectMethod, string parameterName, string fieldName) =>
        injectMethod.ParameterDeclarations
            .Any(p => p.DeclaredName == parameterName);

    private bool IsPrivateField(IFieldDeclaration fieldDeclaration) {
        AccessRights accessRights = fieldDeclaration.GetAccessRights();
        return accessRights == AccessRights.PRIVATE || accessRights == AccessRights.NONE;
    }

    private bool InheritsFromMonoBehaviour(IClassDeclaration classDeclaration) {
        IClass declaredElement = classDeclaration.DeclaredElement;
        if (declaredElement == null)
            return false;

        IDeclaredType[] superTypes = declaredElement.GetAllSuperTypes();
        return superTypes.Any(superType =>
            superType.GetClrName().FullName == "UnityEngine.MonoBehaviour");
    }

    protected override Action<ITextControl> ExecutePsiTransaction(ISolution solution, IProgressIndicator progress) {
        IFieldDeclaration fieldDeclaration = provider.GetSelectedElement<IFieldDeclaration>();
        if (fieldDeclaration == null)
            return null;

        IClassDeclaration containingClass = fieldDeclaration.GetContainingTypeDeclaration() as IClassDeclaration;
        if (containingClass == null)
            return null;

        string fieldName = fieldDeclaration.DeclaredName;
        IType fieldType = fieldDeclaration.Type;
        string parameterName = GetParameterName(fieldName);
        string typeName = fieldType.GetPresentableName(CSharpLanguage.Instance);

        CSharpElementFactory factory = CSharpElementFactory.GetInstance(fieldDeclaration);

        IMethodDeclaration injectMethod = FindInjectDependenciesMethod(containingClass);

        if (injectMethod != null)
            UpdateInjectMethod(factory, injectMethod, fieldType, parameterName, fieldName);
        else
            CreateInjectMethod(factory, containingClass, typeName, parameterName, fieldName);

        return textControl => { };
    }

    private void CreateInjectMethod(CSharpElementFactory factory, IClassDeclaration classDeclaration,
                                          string typeName, string parameterName, string fieldName) {
        try {
            string methodText = $@"[Inject]
private void InjectDependencies({typeName} {parameterName})
{{
    {fieldName} = {parameterName};
}}";

            IMethodDeclaration methodDeclaration = factory.CreateTypeMemberDeclaration(methodText) as IMethodDeclaration;

            if (methodDeclaration != null)
                classDeclaration.AddClassMemberDeclaration(methodDeclaration);
        }
        catch (System.Exception ex) {
            System.Diagnostics.Debug.WriteLine($"PSI method creation failed: {ex.Message}");
        }
    }
    
    private void UpdateInjectMethod(CSharpElementFactory factory, IMethodDeclaration method, 
                                          IType fieldType, string parameterName, string fieldName)
    {
        ICSharpParameterDeclaration existingParam = method.ParameterDeclarations
            .FirstOrDefault(p => p.DeclaredName == parameterName);

        if (existingParam != null)
            return;

        ICSharpParameterDeclaration parameter = factory.CreateParameterDeclaration(
            method,              // ownerDeclaration
            ParameterKind.VALUE, // kind
            false,              // isParams
            false,              // isVarArg
            fieldType,          // type
            parameterName,      // name
            null);              // defaultValue

        ICSharpParameterDeclaration lastParam = method.ParameterDeclarations.LastOrDefault();
        if (lastParam != null)
            method.AddParameterDeclarationAfter(parameter, lastParam);
        else
            method.AddParameterDeclarationBefore(parameter, null);

        string assignmentText = $"{fieldName} = {parameterName};";
        ICSharpStatement assignment = factory.CreateStatement(assignmentText);

        IBlock body = method.Body;
        if (body != null)
        {
            ICSharpStatement lastStatement = body.Statements.LastOrDefault();
            if (lastStatement != null)
                body.AddStatementAfter(assignment, lastStatement);
            else
                body.AddStatementBefore(assignment, null);
        }
    }

    private string GetParameterName(string fieldName) {
        if (fieldName.StartsWith("_"))
            return char.ToLower(fieldName[1]) + fieldName.Substring(2);

        return char.ToLower(fieldName[0]) + fieldName.Substring(1);
    }

    private IMethodDeclaration FindInjectDependenciesMethod(IClassDeclaration classDeclaration) {
        return classDeclaration.MethodDeclarations.FirstOrDefault(method => {
            bool hasInjectAttribute = method.AttributeSectionList?.Sections
                .SelectMany(section => section.Attributes)
                .Any(attr => attr.Name?.NameIdentifier?.Name == "Inject") ?? false;

            bool isInjectDependenciesMethod = method.DeclaredName == "InjectDependencies";

            return hasInjectAttribute && isInjectDependenciesMethod;
        });
    }
}