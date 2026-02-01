using System.Data.SqlClient;
using System.Runtime.ExceptionServices;
using Core.Db;
using Core.Repositories.Exceptions;
using Core.Repositories.Infrastructure.Base;

namespace Core.Repositories.Infrastructure.Base
{
    public abstract class ScalarValuedFunctionsBase : DbObjectWithSchemasBase
    {
        public ScalarValuedFunctionsBase(Session session) : base(session) { }

        public long? SelectLongFromFunction(string fullNameWithParamNames, DbParameterWrap[] parameters = null) => this.SelectValueFromFunction<long?>(fullNameWithParamNames, parameters);
        public Task<long?> SelectLongFromFunctionAsync(string fullNameWithParamNames, CancellationToken cancellationToken) => this.SelectValueFromFunctionAsync<long?>(fullNameWithParamNames, (DbParameterWrap[])null, cancellationToken);
        public Task<long?> SelectLongFromFunctionAsync(string fullNameWithParamNames, DbParameterWrap[] parameters, CancellationToken cancellationToken) => this.SelectValueFromFunctionAsync<long?>(fullNameWithParamNames, parameters, cancellationToken);


        public int? SelectIntegerFromFunction(string fullNameWithParamNames, DbParameterWrap[] parameters = null) => this.SelectValueFromFunction<int?>(fullNameWithParamNames, parameters);
        public Task<int?> SelectIntegerFromFunctionAsync(string fullNameWithParamNames, CancellationToken cancellationToken) => this.SelectValueFromFunctionAsync<int?>(fullNameWithParamNames, (DbParameterWrap[])null, cancellationToken);
        public Task<int?> SelectIntegerFromFunctionAsync(string fullNameWithParamNames, DbParameterWrap[] parameters, CancellationToken cancellationToken) => this.SelectValueFromFunctionAsync<int?>(fullNameWithParamNames, parameters, cancellationToken);


        public string SelectStringFromFunction(string fullNameWithParamNames, DbParameterWrap[] parameters = null) => this.SelectValueFromFunction<string>(fullNameWithParamNames, parameters);
        public Task<string> SelectStringFromFunctionAsync(string fullNameWithParamNames, CancellationToken cancellationToken) => this.SelectValueFromFunctionAsync<string>(fullNameWithParamNames, (DbParameterWrap[])null, cancellationToken);
        public Task<string> SelectStringFromFunctionAsync(string fullNameWithParamNames, DbParameterWrap[] parameters, CancellationToken cancellationToken) => this.SelectValueFromFunctionAsync<string>(fullNameWithParamNames, parameters, cancellationToken);


        public bool? SelectBooleanFromFunction(string fullNameWithParamNames, DbParameterWrap[] parameters = null) => this.SelectValueFromFunction<bool?>(fullNameWithParamNames, parameters);
        public Task<bool?> SelectBooleanFromFunctionAsync(string fullNameWithParamNames, CancellationToken cancellationToken) => this.SelectValueFromFunctionAsync<bool?>(fullNameWithParamNames, (DbParameterWrap[])null, cancellationToken);
        public Task<bool?> SelectBooleanFromFunctionAsync(string fullNameWithParamNames, DbParameterWrap[] parameters, CancellationToken cancellationToken) => this.SelectValueFromFunctionAsync<bool?>(fullNameWithParamNames, parameters, cancellationToken);

        public decimal? SelectDecimalFromFunction(string fullNameWithParamNames, DbParameterWrap[] parameters = null) => this.SelectValueFromFunction<decimal?>(fullNameWithParamNames, parameters);
        public Task<decimal?> SelectDecimalFromFunctionAsync(string fullNameWithParamNames, CancellationToken cancellationToken) => this.SelectValueFromFunctionAsync<decimal?>(fullNameWithParamNames, (DbParameterWrap[])null, cancellationToken);
        public Task<decimal?> SelectDecimalFromFunctionAsync(string fullNameWithParamNames, DbParameterWrap[] parameters, CancellationToken cancellationToken) => this.SelectValueFromFunctionAsync<decimal?>(fullNameWithParamNames, parameters, cancellationToken);


        public DateTimeOffset? SelectDateTimeOffsetFromFunction(string fullNameWithParamNames, DbParameterWrap[] parameters = null) => this.SelectValueFromFunction<DateTimeOffset?>(fullNameWithParamNames, parameters);
        public Task<DateTimeOffset?> SelectDateTimeOffsetFromFunctionAsync(string fullNameWithParamNames, CancellationToken cancellationToken) => this.SelectValueFromFunctionAsync<DateTimeOffset?>(fullNameWithParamNames, (DbParameterWrap[])null, cancellationToken);
        public Task<DateTimeOffset?> SelectDateTimeOffsetFromFunctionAsync(string fullNameWithParamNames, DbParameterWrap[] parameters, CancellationToken cancellationToken) => this.SelectValueFromFunctionAsync<DateTimeOffset?>(fullNameWithParamNames, parameters, cancellationToken);


        public Guid? SelectGuidFromFunction(string fullNameWithParamNames, DbParameterWrap[] parameters = null) => this.SelectValueFromFunction<Guid?>(fullNameWithParamNames, parameters);
        public Task<Guid?> SelectGuidFromFunctionAsync(string fullNameWithParamNames, CancellationToken cancellationToken) => this.SelectValueFromFunctionAsync<Guid?>(fullNameWithParamNames, (DbParameterWrap[])null, cancellationToken);
        public Task<Guid?> SelectGuidFromFunctionAsync(string fullNameWithParamNames, DbParameterWrap[] parameters, CancellationToken cancellationToken) => this.SelectValueFromFunctionAsync<Guid?>(fullNameWithParamNames, parameters, cancellationToken);

        private TReturnScalarType SelectValueFromFunction<TReturnScalarType>(string fullNameWithParamNames, DbParameterWrap[] parameters = null)
            => this.SelectValueFromFunction<TReturnScalarType>(fullNameWithParamNames, ToSqlParameters(parameters));
        private TReturnScalarType SelectValueFromFunction<TReturnScalarType>(string fullNameWithParamNames, SqlParameter[] parameters = null)
        {
            try
            {
                var value = this.session.Sql(session => session.SQLScalar($"SELECT {fullNameWithParamNames}", parameters));
                if (value == DBNull.Value)
                    return default;

                var typedValue = (TReturnScalarType)value;
                return typedValue;
            }
            catch (Exception ex)
            {
                ExceptionDispatchInfo.Capture(new RepositoryException(ex.Message, ex)).Throw();
            }

            // runtime will not reach this line
            return default;
        }


        private Task<TReturnScalarType> SelectValueFromFunctionAsync<TReturnScalarType>(string fullNameWithParamNames, DbParameterWrap[] parameters, CancellationToken cancellationToken)
            => this.SelectValueFromFunctionAsync<TReturnScalarType>(fullNameWithParamNames, ToSqlParameters(parameters), cancellationToken);
        private async Task<TReturnScalarType> SelectValueFromFunctionAsync<TReturnScalarType>(string fullNameWithParamNames, SqlParameter[] parameters, CancellationToken cancellationToken)
        {
            try
            {
                var value = await this.session.SqlAsync((session, ct) => session.SQLScalarAsync($"SELECT {fullNameWithParamNames}", parameters, ct), cancellationToken);
                if (value == DBNull.Value)
                    return default;

                var typedValue = (TReturnScalarType)value;
                return typedValue;
            }
            catch (OperationCanceledException)
            {
                throw;
            }
            catch (Exception ex)
            {
                ExceptionDispatchInfo.Capture(new RepositoryException(ex.Message, ex)).Throw();
            }

            // runtime will not reach this line
            return default;
        }



        public new abstract class SchemaBase : DbObjectWithSchemasBase.SchemaBase
        {
            public SchemaBase(Session session, ScalarValuedFunctionsBase scalarValuedFunctions) : base(session)
            {
                this.ScalarValuedFunctions = scalarValuedFunctions;
            }

            public ScalarValuedFunctionsBase ScalarValuedFunctions { get; private set; }
        }
    }
}
