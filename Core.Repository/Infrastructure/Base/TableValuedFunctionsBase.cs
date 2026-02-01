using System.Data;
using System.Data.SqlClient;
using System.Runtime.ExceptionServices;
using Core.Db;
using Core.Repositories.Exceptions;

namespace Core.Repositories.Infrastructure.Base
{
    public abstract class TableValuedFunctionsBase : DbObjectWithSchemasBase
    {
        public TableValuedFunctionsBase(Session session) : base(session) { }

        public DataTable SelectTableFromFunction(string fullNameWithParamNames, DbParameterWrap[] parameters = null)
            => this.SelectTableFromFunction(fullNameWithParamNames, ToSqlParameters(parameters));
        private DataTable SelectTableFromFunction(string fullNameWithParamNames, SqlParameter[] parameters = null)
        {
            DataSet ds = null;
            try
            {
                ds = this.session.Sql(session => session.SQLTextOpen($"SELECT * FROM {fullNameWithParamNames}", parameters));
            }
            catch (Exception ex)
            {
                ExceptionDispatchInfo.Capture(new RepositoryException(ex.Message, ex)).Throw();
            }
            if (ds.Tables.Count == 0)
                throw new RepositoryException("DataSet doesn't contain tables. At least one table should be in the set");

            return ds.Tables[0];
        }


        public Task<DataTable> SelectTableFromFunctionAsync(string fullNameWithParamNames, CancellationToken cancellationToken)
            => this.SelectTableFromFunctionAsync(fullNameWithParamNames, (DbParameterWrap[])null, cancellationToken);
        public Task<DataTable> SelectTableFromFunctionAsync(string fullNameWithParamNames, DbParameterWrap[] parameters, CancellationToken cancellationToken)
            => this.SelectTableFromFunctionAsync(fullNameWithParamNames, ToSqlParameters(parameters), cancellationToken);
        private async Task<DataTable> SelectTableFromFunctionAsync(string fullNameWithParamNames, SqlParameter[] parameters, CancellationToken cancellationToken)
        {
            DataSet ds = null;
            try
            {
                ds = await this.session.SqlAsync((session, ct) => session.SQLTextOpenAsync($"SELECT * FROM {fullNameWithParamNames}", parameters, ct), cancellationToken);
            }
            catch (OperationCanceledException)
            {
                throw;
            }
            catch (Exception ex)
            {
                ExceptionDispatchInfo.Capture(new RepositoryException(ex.Message, ex)).Throw();
            }
            if (ds.Tables.Count == 0)
                throw new RepositoryException("DataSet doesn't contain tables. At least one table should be in the set");

            return ds.Tables[0];
        }




        public new abstract class SchemaBase : DbObjectWithSchemasBase.SchemaBase
        {
            public SchemaBase(Session session, TableValuedFunctionsBase tableValuedFunctions) : base(session)
            {
                this.TableValuedFunctions = tableValuedFunctions;
            }

            public TableValuedFunctionsBase TableValuedFunctions { get; private set; }
        }
    }
}
