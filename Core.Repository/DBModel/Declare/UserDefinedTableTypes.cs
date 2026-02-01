#pragma warning disable IDE1006 // Naming Styles

namespace Core.Repositories.DBModel.Declare
{
    public abstract class UserDefinedTableTypes
    {
        public abstract class dbo
        {
            public const string ud_list_int = "[dbo].[ud_list_int]";
            public const string ud_list_bigint = "[dbo].[ud_list_bigint]";
            public const string ud_list_guid = "[dbo].[ud_list_guid]";
            public const string ud_list_nvarchar = "[dbo].[ud_list_nvarchar]";
            public const string ud_list_pair_guid_guid = "[dbo].[ud_list_pair_guid_guid]";
            public const string ud_list_pair_nvarchar_int = "[dbo].[ud_list_pair_nvarchar_int]";
            public const string ud_list_pair_bigint_int = "[dbo].[ud_list_pair_bigint_int]";
            public const string ud_list_pair_bigint_bigint = "[dbo].[ud_list_pair_bigint_bigint]";
            public const string ud_list_pair_bigint_datetimeoffset = "[dbo].[ud_list_pair_bigint_datetimeoffset]";
        }
    }
}
