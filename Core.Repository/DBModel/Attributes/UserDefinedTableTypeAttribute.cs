using System;

namespace Core.Repositories.DBModel.Attributes
{
    [AttributeUsage(AttributeTargets.Class, AllowMultiple = false)]
    public class UserDefinedTableTypeAttribute : Attribute
    {
        public string TypeName { get; }

        /// <summary>
        /// 
        /// </summary>
        /// <param name="typeName">For example: [ops].[fact_operation_accounts_type]</param>
        public UserDefinedTableTypeAttribute(string typeName)
        {
            this.TypeName = typeName;
        }
    }
}
