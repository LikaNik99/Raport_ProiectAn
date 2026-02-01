using System.Data;
using System.Data.SqlClient;
using System.Reflection;
using Core.Db;
using Core.Repositories.DBModel.Attributes;
using Core.Repositories.DBModel.Declare;
using Core.Utils;
using static Core.Repositories.Infrastructure.Base.RepositoryBase;

namespace Core.Repositories.Infrastructure.Base
{
    public abstract class DbObjectBase
    {
        protected Session session;
        public DbObjectBase(Session session)
        {
            this.session = session;
        }


        /// <summary>
        /// Converts data table into items list of the specified value type
        /// </summary>
        /// <typeparam name="T"></typeparam>
        /// <param name="dt"></param>
        /// <returns></returns>
        protected static IList<T?> ToList<T>(DataTable dt) where T : struct
        {
            var result = new List<T?>();

            foreach (DataRow row in dt.Rows)
            {
                var dbValue = row.ItemArray[0];
                if (dbValue == DBNull.Value)
                    dbValue = null;

                T? value;
                if (typeof(T).IsEnum)
                    value = (T?)Enum.Parse(typeof(T), dbValue.ToString());      // TODO: Rewrite. ToString() and Parse() is not good solution
                else
                    value = (T?)dbValue;

                result.Add(value);
            }

            return result;
        }


        /// <summary>
        /// Converts data table into items list of the specified type
        /// </summary>
        /// <typeparam name="TItemEntity"></typeparam>
        /// <param name="dt"></param>
        /// <param name="doNotTransform"></param>
        /// <returns></returns>
        protected static IList<TItemEntity> ToList<TItemEntity>(DataTable dt, bool doNotTransform = false) where TItemEntity : class, new()
        {
            List<TItemEntity> entities = null;
            if (doNotTransform)
                entities = PPDataTableExtensions.ToList<TItemEntity>(dt);
            else
                entities = PPDataTableExtensions.ToList<TItemEntity>(dt, PPDataTableExtensions.NameTransformType.CamelCaseIntoPascalCase);
            return entities;
        }


        // TODO: only TotalCount? It's hard to extend in future. It would be better to use stored procedures with out parameters, even for simple select queries.
        /// <summary>
        /// Converts data table into items list of the specified type, wrapped in the object, which also includes TotalCount property, taken from the data table for the specified column <paramref name="totalCountColumnName"/> from the first row
        /// </summary>
        /// <typeparam name="TItemEntity"></typeparam>
        /// <param name="dt"></param>
        /// <param name="totalCountColumnName"></param>
        /// <returns></returns>
        protected static DbResultList<TItemEntity> ToDbResult<TItemEntity>(DataTable dt, string totalCountColumnName = "TotalCount") where TItemEntity : class, new()
        {
            var entities = ToList<TItemEntity>(dt);

            var totalCount = 0;
            if (dt.Rows.Count > 0)
                totalCount = (int)dt.Rows[0]["TotalCount"];

            var dbResult = new DbResultList<TItemEntity>
            {
                TotalCount = totalCount,
                ResultItems = entities,
            };

            return dbResult;
        }


        /// <summary>
        /// Converts items list into data table. Creates data table if null parameter is passed
        /// </summary>
        /// <typeparam name="TItemEntity"></typeparam>
        /// <param name="items"></param>
        /// <returns></returns>
        private static DataTable ToDataTable<TItemEntity>(IEnumerable<TItemEntity> items) where TItemEntity : class
        {
            if (items == null)
                items = new List<TItemEntity>();

            var dt = PPDataTableExtensions.CreateDataTable(items, PPDataTableExtensions.NameTransformType.PascalCaseIntoCamelCase);
            return dt;
        }


        /// <summary>
        /// Gets value from sql parameter
        /// </summary>
        /// <typeparam name="TValue"></typeparam>
        /// <param name="parameter"></param>
        /// <returns></returns>
        protected static Nullable<TValue> ToValue<TValue>(DbOutputParameterWrap parameter) where TValue : struct => parameter.GetValue<TValue>();


        /// <summary>
        /// Gets value from sql parameter
        /// </summary>
        /// <param name="parameter"></param>
        /// <returns></returns>
        protected static string ToValueString(DbOutputParameterWrap parameter) => parameter.GetValueString();


        /// <summary>
        /// Converts list or array of strings into data table, which corresponds to the sql defined type <see cref="UserDefinedTableTypes.dbo.ud_list_nvarchar"/> and then can be used as sql parameter
        /// </summary>
        /// <param name="stringValues"></param>
        /// <returns></returns>
        private static DataTable ToUserDefinedListDt(IEnumerable<string> stringValues)
        {
            if (stringValues is null)
                stringValues = Array.Empty<string>();

            var dt = new DataTable();
            dt.Columns.Add("value", typeof(string));
            foreach (var v in stringValues)
                dt.Rows.Add(v);

            return dt;
        }


        /// <summary>
        /// Converts list or array of integers into data table, which corresponds to the sql defined type <see cref="UserDefinedTableTypes.dbo.ud_list_int"/> and then can be used as sql parameter
        /// </summary>
        /// <param name="integerValues"></param>
        /// <returns></returns>
        private static DataTable ToUserDefinedListDt(IEnumerable<int> integerValues)
        {
            if (integerValues is null)
                integerValues = Array.Empty<int>();

            var dt = new DataTable();
            dt.Columns.Add("value", typeof(int));
            foreach (var v in integerValues)
                dt.Rows.Add(v);

            return dt;
        }


        /// <summary>
        /// Converts list or array of integers into data table, which corresponds to the sql defined type <see cref="UserDefinedTableTypes.dbo.ud_list_bigint"/> and then can be used as sql parameter
        /// </summary>
        /// <param name="longValues"></param>
        /// <returns></returns>
        private static DataTable ToUserDefinedListDt(IEnumerable<long> longValues)
        {
            if (longValues is null)
                longValues = Array.Empty<long>();

            var dt = new DataTable();
            dt.Columns.Add("value", typeof(long));
            foreach (var v in longValues)
                dt.Rows.Add(v);

            return dt;
        }


        /// <summary>
        /// Converts list or array of guids (uniqueidentifier) into data table, which corresponds to the sql defined type <see cref="UserDefinedTableTypes.dbo.ud_list_guid"/> and then can be used as sql parameter
        /// </summary>
        /// <param name="guidValues"></param>
        /// <returns></returns>
        private static DataTable ToUserDefinedListDt(IEnumerable<Guid> guidValues)
        {
            if (guidValues is null)
                guidValues = Array.Empty<Guid>();

            var dt = new DataTable();
            dt.Columns.Add("key", typeof(Guid));
            foreach (var v in guidValues)
                dt.Rows.Add(v);

            return dt;
        }


        /// <summary>
        /// Converts list or array of guids (uniqueidentifier) into data table, which corresponds to the sql defined type <see cref="UserDefinedTableTypes.dbo.ud_list_pair_guid_guid"/> and then can be used as sql parameter
        /// </summary>
        /// <param name="pairs"></param>
        /// <returns></returns>
        private static DataTable ToUserDefinedListDt(IEnumerable<KeyValuePair<Guid, Guid?>> pairs)
        {
            if (pairs is null)
                pairs = Array.Empty<KeyValuePair<Guid, Guid?>>();

            var dt = new DataTable();
            dt.Columns.Add("key", typeof(Guid));
            dt.Columns.Add("value", typeof(Guid));
            foreach (var p in pairs)
                dt.Rows.Add(p.Key, p.Value);

            return dt;
        }


        /// <summary>
        /// Converts list or array of guids (uniqueidentifier) into data table, which corresponds to the sql defined type <see cref="UserDefinedTableTypes.dbo.ud_list_pair_nvarchar_int"/> and then can be used as sql parameter
        /// </summary>
        /// <param name="pairs"></param>
        /// <returns></returns>
        private static DataTable ToUserDefinedListDt(IEnumerable<KeyValuePair<string, int?>> pairs)
        {
            if (pairs is null)
                pairs = Array.Empty<KeyValuePair<string, int?>>();

            var dt = new DataTable();
            dt.Columns.Add("key", typeof(string));
            dt.Columns.Add("value", typeof(int));
            foreach (var p in pairs)
                dt.Rows.Add(p.Key, p.Value);

            return dt;
        }


        /// <summary>
        /// Converts list or array of long integers into data table, which corresponds to the sql defined type <see cref="UserDefinedTableTypes.dbo.ud_list_pair_bigint_int"/> and then can be used as sql parameter
        /// </summary>
        /// <param name="pairs"></param>
        /// <returns></returns>
        private static DataTable ToUserDefinedListDt(IEnumerable<KeyValuePair<long, int?>> pairs)
        {
            if (pairs is null)
                pairs = Array.Empty<KeyValuePair<long, int?>>();

            var dt = new DataTable();
            dt.Columns.Add("key", typeof(long));
            dt.Columns.Add("value", typeof(int));
            foreach (var p in pairs)
                dt.Rows.Add(p.Key, p.Value);

            return dt;
        }


        /// <summary>
        /// Converts list or array of long integers into data table, which corresponds to the sql defined type <see cref="UserDefinedTableTypes.dbo.ud_list_pair_bigint_bigint"/> and then can be used as sql parameter
        /// </summary>
        /// <param name="pairs"></param>
        /// <returns></returns>
        private static DataTable ToUserDefinedListDt(IEnumerable<KeyValuePair<long, long?>> pairs)
        {
            if (pairs is null)
                pairs = Array.Empty<KeyValuePair<long, long?>>();

            var dt = new DataTable();
            dt.Columns.Add("key", typeof(long));
            dt.Columns.Add("value", typeof(long));
            foreach (var p in pairs)
                dt.Rows.Add(p.Key, p.Value);

            return dt;
        }


        /// <summary>
        /// Converts list or array of long integers into data table, which corresponds to the sql defined type <see cref="UserDefinedTableTypes.dbo.ud_list_pair_bigint_datetimeoffset"/> and then can be used as sql parameter
        /// </summary>
        /// <param name="pairs"></param>
        /// <returns></returns>
        private static DataTable ToUserDefinedListDt(IEnumerable<KeyValuePair<long, DateTimeOffset?>> pairs)
        {
            if (pairs is null)
                pairs = Array.Empty<KeyValuePair<long, DateTimeOffset?>>();

            var dt = new DataTable();
            dt.Columns.Add("key", typeof(long));
            dt.Columns.Add("value", typeof(DateTimeOffset));
            foreach (var p in pairs)
                dt.Rows.Add(p.Key, p.Value);

            return dt;
        }



        protected static DbOutputParameterWrap CreateOutputParameter(string parameterName, SqlDbType dbType) => new DbOutputParameterWrap(parameterName, dbType);
        protected static DbParameterWrap CreateParameter(string parameterName, bool? value) => new DbParameterWrap(parameterName, value);
        protected static DbParameterWrap CreateParameter(string parameterName, short? value) => new DbParameterWrap(parameterName, value);
        protected static DbParameterWrap CreateParameter(string parameterName, int? value) => new DbParameterWrap(parameterName, value);
        protected static DbParameterWrap CreateParameter(string parameterName, long? value) => new DbParameterWrap(parameterName, value);
        protected static DbParameterWrap CreateParameter(string parameterName, decimal? value) => new DbParameterWrap(parameterName, value);
        protected static DbParameterWrap CreateParameter(string parameterName, Guid? value) => new DbParameterWrap(parameterName, value);
        protected static DbParameterWrap CreateParameter(string parameterName, DateTimeOffset? value) => new DbParameterWrap(parameterName, value);
        protected static DbParameterWrap CreateParameter(string parameterName, DateTime? value) => new DbParameterWrap(parameterName, value);
        protected static DbParameterWrap CreateParameter(string parameterName, string value) => new DbParameterWrap(parameterName, value);
        protected static DbParameterWrap CreateParameter(string parameterName, Enum value) => new DbParameterWrap(parameterName, value);
        protected static DbParameterWrap CreateParameter(string parameterName, IEnumerable<byte> value) => new DbParameterWrap(parameterName, value);
        protected static DbParameterWrap CreateParameter(string parameterName, DataTable value, string typeName = null) => new DbParameterWrap(parameterName, value, typeName: typeName);
        protected static DbParameterWrap CreateParameter<TItemEntity>(string parameterName, IEnumerable<TItemEntity> value, string typeName = null) where TItemEntity : class
        {
            var dtValue = ToDataTable(value);

            if (typeName is null)
            {
                var attr = typeof(TItemEntity).GetCustomAttribute<UserDefinedTableTypeAttribute>();
                typeName = attr?.TypeName;
            }

            var parameter = new DbParameterWrap(parameterName, dtValue, typeName);
            return parameter;
        }


        /// <summary>
        /// Actually using <see cref="UserDefinedTableTypes.dbo.ud_list_int"/> user-defined table type
        /// </summary>
        /// <param name="parameterName"></param>
        /// <param name="value"></param>
        /// <returns></returns>
        protected static DbParameterWrap CreateParameter(string parameterName, IEnumerable<int> value)
        {
            var valueDt = ToUserDefinedListDt(value);
            var parameter = new DbParameterWrap(parameterName, valueDt, UserDefinedTableTypes.dbo.ud_list_int);
            return parameter;
        }

        /// <summary>
        /// Actually using <see cref="UserDefinedTableTypes.dbo.ud_list_int"/> user-defined table type
        /// </summary>
        /// <param name="parameterName"></param>
        /// <param name="value"></param>
        /// <returns></returns>
        protected static DbParameterWrap CreateParameter(string parameterName, IEnumerable<int?> value) => CreateParameter(parameterName, value?.Where(v => v.HasValue).Select(v => (int)v));

        /// <summary>
        /// Actually using <see cref="UserDefinedTableTypes.dbo.ud_list_bigint"/> user-defined table type
        /// </summary>
        /// <param name="parameterName"></param>
        /// <param name="value"></param>
        /// <returns></returns>
        protected static DbParameterWrap CreateParameter(string parameterName, IEnumerable<long> value)
        {
            var valueDt = ToUserDefinedListDt(value);
            var parameter = new DbParameterWrap(parameterName, valueDt, UserDefinedTableTypes.dbo.ud_list_bigint);
            return parameter;
        }

        /// <summary>
        /// Actually using <see cref="UserDefinedTableTypes.dbo.ud_list_bigint"/> user-defined table type
        /// </summary>
        /// <param name="parameterName"></param>
        /// <param name="value"></param>
        /// <returns></returns>
        protected static DbParameterWrap CreateParameter(string parameterName, IEnumerable<long?> value) => CreateParameter(parameterName, value?.Where(v => v.HasValue).Select(v => (long)v));

        /// <summary>
        /// Actually using <see cref="UserDefinedTableTypes.dbo.ud_list_guid"/> user-defined table type
        /// </summary>
        /// <param name="parameterName"></param>
        /// <param name="value"></param>
        /// <returns></returns>
        protected static DbParameterWrap CreateParameter(string parameterName, IEnumerable<Guid> value)
        {
            var valueDt = ToUserDefinedListDt(value);
            var parameter = new DbParameterWrap(parameterName, valueDt, UserDefinedTableTypes.dbo.ud_list_guid);
            return parameter;
        }

        /// <summary>
        /// Actually using <see cref="UserDefinedTableTypes.dbo.ud_list_guid"/> user-defined table type
        /// </summary>
        /// <param name="parameterName"></param>
        /// <param name="value"></param>
        /// <returns></returns>
        protected static DbParameterWrap CreateParameter(string parameterName, IEnumerable<Guid?> value) => CreateParameter(parameterName, value?.Where(v => v.HasValue).Select(v => (Guid)v));

        /// <summary>
        /// Actually using <see cref="UserDefinedTableTypes.dbo.ud_list_nvarchar"/> user-defined table type. No more than 50 characters can be in a string item
        /// </summary>
        /// <param name="parameterName"></param>
        /// <param name="value"></param>
        /// <returns></returns>
        protected static DbParameterWrap CreateParameter(string parameterName, IEnumerable<string> value)
        {
            var valueDt = ToUserDefinedListDt(value);
            var parameter = new DbParameterWrap(parameterName, valueDt, UserDefinedTableTypes.dbo.ud_list_nvarchar);
            return parameter;
        }

        /// <summary>
        /// Actually using <see cref="UserDefinedTableTypes.dbo.ud_list_pair_guid_guid"/> user-defined table type. No more than 50 characters can be in a string item
        /// </summary>
        /// <param name="parameterName"></param>
        /// <param name="value"></param>
        /// <returns></returns>
        protected static DbParameterWrap CreateParameter(string parameterName, IEnumerable<KeyValuePair<Guid, Guid?>> value)
        {
            var valueDt = ToUserDefinedListDt(value);
            var parameter = new DbParameterWrap(parameterName, valueDt, UserDefinedTableTypes.dbo.ud_list_pair_guid_guid);
            return parameter;
        }

        /// <summary>
        /// Actually using <see cref="UserDefinedTableTypes.dbo.ud_list_pair_guid_guid"/> user-defined table type. No more than 50 characters can be in a string item
        /// </summary>
        /// <param name="parameterName"></param>
        /// <param name="value"></param>
        /// <returns></returns>
        protected static DbParameterWrap CreateParameter(string parameterName, IEnumerable<KeyValuePair<Guid, Guid>> value) => CreateParameter(parameterName, value?.Select(p => new KeyValuePair<Guid, Guid?>(p.Key, p.Value)));

        /// <summary>
        /// Actually using <see cref="UserDefinedTableTypes.dbo.ud_list_pair_nvarchar_int"/> user-defined table type. No more than 50 characters can be in a string item
        /// </summary>
        /// <param name="parameterName"></param>
        /// <param name="value"></param>
        /// <returns></returns>
        protected static DbParameterWrap CreateParameter(string parameterName, IEnumerable<KeyValuePair<string, int?>> value)
        {
            var valueDt = ToUserDefinedListDt(value);
            var parameter = new DbParameterWrap(parameterName, valueDt, UserDefinedTableTypes.dbo.ud_list_pair_nvarchar_int);
            return parameter;
        }

        /// <summary>
        /// Actually using <see cref="UserDefinedTableTypes.dbo.ud_list_pair_nvarchar_int"/> user-defined table type. No more than 50 characters can be in a string item
        /// </summary>
        /// <param name="parameterName"></param>
        /// <param name="value"></param>
        /// <returns></returns>
        protected static DbParameterWrap CreateParameter(string parameterName, IEnumerable<KeyValuePair<string, int>> value) => CreateParameter(parameterName, value?.Select(p => new KeyValuePair<string, int?>(p.Key, p.Value)));

        /// <summary>
        /// Actually using <see cref="UserDefinedTableTypes.dbo.ud_list_pair_nvarchar_int"/> user-defined table type. No more than 50 characters can be in a string item
        /// </summary>
        /// <param name="parameterName"></param>
        /// <param name="value"></param>
        /// <returns></returns>
        protected static DbParameterWrap CreateParameter<TEnum>(string parameterName, IEnumerable<KeyValuePair<string, TEnum?>> value) where TEnum : struct => CreateParameter(parameterName, value.Select(p => new KeyValuePair<string, int?>(p.Key, p.Value is null ? (int?)null : (int)(object)p.Value)));

        /// <summary>
        /// Actually using <see cref="UserDefinedTableTypes.dbo.ud_list_pair_nvarchar_int"/> user-defined table type. No more than 50 characters can be in a string item
        /// </summary>
        /// <param name="parameterName"></param>
        /// <param name="value"></param>
        /// <returns></returns>
        protected static DbParameterWrap CreateParameter<TEnum>(string parameterName, IEnumerable<KeyValuePair<string, TEnum>> value) where TEnum : struct => CreateParameter(parameterName, value.Select(p => new KeyValuePair<string, int>(p.Key, (int)(object)p.Value)));

        /// <summary>
        /// Actually using <see cref="UserDefinedTableTypes.dbo.ud_list_pair_bigint_int"/> user-defined table type
        /// </summary>
        /// <param name="parameterName"></param>
        /// <param name="value"></param>
        /// <returns></returns>
        protected static DbParameterWrap CreateParameter(string parameterName, IEnumerable<KeyValuePair<long, int?>> value)
        {
            var valueDt = ToUserDefinedListDt(value);
            var parameter = new DbParameterWrap(parameterName, valueDt, UserDefinedTableTypes.dbo.ud_list_pair_bigint_int);
            return parameter;
        }

        /// <summary>
        /// Actually using <see cref="UserDefinedTableTypes.dbo.ud_list_pair_bigint_int"/> user-defined table type
        /// </summary>
        /// <param name="parameterName"></param>
        /// <param name="value"></param>
        /// <returns></returns>
        protected static DbParameterWrap CreateParameter(string parameterName, IEnumerable<KeyValuePair<long, int>> value) => CreateParameter(parameterName, value?.Select(p => new KeyValuePair<long, int?>(p.Key, p.Value)));

        /// <summary>
        /// Actually using <see cref="UserDefinedTableTypes.dbo.ud_list_pair_bigint_int"/> user-defined table type
        /// </summary>
        /// <param name="parameterName"></param>
        /// <param name="value"></param>
        /// <returns></returns>
        protected static DbParameterWrap CreateParameter<TEnum>(string parameterName, IEnumerable<KeyValuePair<long, TEnum?>> value) where TEnum : struct => CreateParameter(parameterName, value.Select(p => new KeyValuePair<long, int?>(p.Key, p.Value is null ? (int?)null : (int)(object)p.Value)));

        /// <summary>
        /// Actually using <see cref="UserDefinedTableTypes.dbo.ud_list_pair_bigint_int"/> user-defined table type
        /// </summary>
        /// <param name="parameterName"></param>
        /// <param name="value"></param>
        /// <returns></returns>
        protected static DbParameterWrap CreateParameter<TEnum>(string parameterName, IEnumerable<KeyValuePair<long, TEnum>> value) where TEnum : struct => CreateParameter(parameterName, value.Select(p => new KeyValuePair<long, int>(p.Key, (int)(object)p.Value)));

        /// <summary>
        /// Actually using <see cref="UserDefinedTableTypes.dbo.ud_list_pair_bigint_bigint"/> user-defined table type. No more than 50 characters can be in a string item
        /// </summary>
        /// <param name="parameterName"></param>
        /// <param name="value"></param>
        /// <returns></returns>
        protected static DbParameterWrap CreateParameter(string parameterName, IEnumerable<KeyValuePair<long, long?>> value)
        {
            var valueDt = ToUserDefinedListDt(value);
            var parameter = new DbParameterWrap(parameterName, valueDt, UserDefinedTableTypes.dbo.ud_list_pair_bigint_bigint);
            return parameter;
        }

        /// <summary>
        /// Actually using <see cref="UserDefinedTableTypes.dbo.ud_list_pair_bigint_bigint"/> user-defined table type. No more than 50 characters can be in a string item
        /// </summary>
        /// <param name="parameterName"></param>
        /// <param name="value"></param>
        /// <returns></returns>
        protected static DbParameterWrap CreateParameter(string parameterName, IEnumerable<KeyValuePair<long, long>> value) => CreateParameter(parameterName, value?.Select(p => new KeyValuePair<long, long?>(p.Key, p.Value)));

        /// <summary>
        /// Actually using <see cref="UserDefinedTableTypes.dbo.ud_list_pair_bigint_datetimeoffset"/> user-defined table type. No more than 50 characters can be in a string item
        /// </summary>
        /// <param name="parameterName"></param>
        /// <param name="value"></param>
        /// <returns></returns>
        protected static DbParameterWrap CreateParameter(string parameterName, IEnumerable<KeyValuePair<long, DateTimeOffset?>> value)
        {
            var valueDt = ToUserDefinedListDt(value);
            var parameter = new DbParameterWrap(parameterName, valueDt, UserDefinedTableTypes.dbo.ud_list_pair_bigint_datetimeoffset);
            return parameter;
        }

        /// <summary>
        /// Actually using <see cref="UserDefinedTableTypes.dbo.ud_list_pair_bigint_datetimeoffset"/> user-defined table type. No more than 50 characters can be in a string item
        /// </summary>
        /// <param name="parameterName"></param>
        /// <param name="value"></param>
        /// <returns></returns>
        protected static DbParameterWrap CreateParameter(string parameterName, IEnumerable<KeyValuePair<long, DateTimeOffset>> value) => CreateParameter(parameterName, value?.Select(p => new KeyValuePair<long, DateTimeOffset?>(p.Key, p.Value)));

        /// <summary>
        /// For numerables of enums. Actually using <see cref="UserDefinedTableTypes.dbo.ud_list_int"/> user-defined table type
        /// </summary>
        /// <param name="parameterName"></param>
        /// <param name="value"></param>
        /// <returns></returns>
        protected static DbParameterWrap CreateParameter<TEnum>(string parameterName, IEnumerable<TEnum> value) where TEnum : struct => CreateParameter(parameterName, value?.Select(v => (int)(object)v));



        /// <summary>
        /// Wrap over SqlParameter
        /// </summary>
        public class DbParameterWrap
        {
            public string ParameterName { get; set; }
            public virtual object Value { get; set; }
            public string TypeName { get; set; }

            public bool IsVarbinary { get; set; }

            public DbParameterWrap(string parameterName, IEnumerable<byte> value)
            {
                this.ParameterName = parameterName;
                this.Value = value;
                this.IsVarbinary = true;
            }

            public DbParameterWrap(string parameterName, object value, string typeName = null)
            {
                this.ParameterName = parameterName;
                this.Value = value;
                this.TypeName = typeName;
            }
        }


        public class DbOutputParameterWrap : DbParameterWrap
        {
            public DbOutputParameterWrap(string parameterName, SqlDbType dbType) : base(parameterName, value: null)
            {
                this.SqlDbType = dbType;
            }

            public SqlDbType SqlDbType { get; set; }

            public override object Value
            {
                get => this.ValueGetter is null ? base.Value : this.ValueGetter();
                set => base.Value = value;
            }

            public Func<object> ValueGetter { get; set; }


            /// <summary>
            /// Gets value from sql parameter
            /// </summary>
            /// <typeparam name="TValue"></typeparam>
            /// <returns></returns>
            public Nullable<TValue> GetValue<TValue>() where TValue : struct
            {
                object parameterValue = this.ValueGetter?.Invoke();

                if (parameterValue == DBNull.Value)
                    return null;

                if (typeof(TValue).IsEnum)
                    return (TValue)(object)(int)parameterValue;

                return (TValue)parameterValue;
            }


            /// <summary>
            /// Gets value from sql parameter
            /// </summary>
            /// <returns></returns>
            public string GetValueString()
            {
                object parameterValue = this.ValueGetter?.Invoke();

                if (parameterValue == DBNull.Value)
                    return null;

                return (string)parameterValue;
            }
        }


        /// <summary>
        /// Convert parameters of our type <see cref="DbParameterWrap"/> to the type <see cref="SqlParameter"/>
        /// </summary>
        /// <param name="parameter"></param>
        /// <returns></returns>
        private static SqlParameter ToSqlParameter(DbParameterWrap parameter)
        {
            var sqlParameter = new SqlParameter(parameter.ParameterName, parameter.Value);

            if (parameter.IsVarbinary)
            {
                sqlParameter.SqlDbType = SqlDbType.VarBinary;
            }

            if (parameter.TypeName != null)
            {
                sqlParameter.TypeName = parameter.TypeName;
                sqlParameter.SqlDbType = SqlDbType.Structured;
            }

            if (parameter is DbOutputParameterWrap outputParameter)
            {
                sqlParameter.Direction = ParameterDirection.Output;
                sqlParameter.SqlDbType = outputParameter.SqlDbType;
                outputParameter.ValueGetter = () => sqlParameter.Value;
            }

            return sqlParameter;
        }


        /// <summary>
        /// Convert array of parameters of our type <see cref="DbParameterWrap"/> to the type <see cref="SqlParameter"/>
        /// </summary>
        /// <param name="parameters"></param>
        /// <returns></returns>
        protected static SqlParameter[] ToSqlParameters(DbParameterWrap[] parameters)
            => parameters?.Select(p => ToSqlParameter(p)).ToArray();
    }
}
