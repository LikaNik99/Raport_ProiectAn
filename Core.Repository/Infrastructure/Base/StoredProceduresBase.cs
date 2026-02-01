using System.Data;
using System.Data.SqlClient;
using System.Runtime.ExceptionServices;
using Core.Db;
using Core.Repositories.Exceptions;

namespace Core.Repositories.Infrastructure.Base
{
    public abstract class StoredProceduresBase : DbObjectWithSchemasBase
    {
        public StoredProceduresBase(Session session) : base(session) { }


        public DataTable SelectTableFromStoredProcedure(string fullName, DbParameterWrap[] parameters)
            => this.SelectTableFromStoredProcedure(fullName, ToSqlParameters(parameters));
        private DataTable SelectTableFromStoredProcedure(string fullName, SqlParameter[] parameters)
        {
            DataSet ds = null;
            try
            {
                ds = this.session.Sql(session => session.StoredProcOpen(fullName, parameters));
            }
            catch (Exception ex)
            {
                ExceptionDispatchInfo.Capture(new RepositoryException(ex.Message, ex)).Throw();
            }



            if (ds.Tables.Count == 0)
                throw new RepositoryException("DataSet doesn't contain tables. At least one table should be in the set");

            return ds.Tables[0];
        }


        public void ExecStoredProcedure(string fullName, DbParameterWrap[] parameters)
            => this.ExecStoredProcedure(fullName, ToSqlParameters(parameters));
        private void ExecStoredProcedure(string fullName, SqlParameter[] parameters)
        {
            try
            {
                var returnParameters = this.session.Sql(session => session.StoredProcExec(fullName, parameters));
            }
            catch (Exception ex)
            {
                ExceptionDispatchInfo.Capture(new RepositoryException(ex.Message, ex)).Throw();
            }
        }



        public Task<DataTable> SelectTableFromStoredProcedureAsync(string fullName, CancellationToken cancellationToken)
            => this.SelectTableFromStoredProcedureAsync(fullName, new DbParameterWrap[0], cancellationToken);
        public Task<DataTable> SelectTableFromStoredProcedureAsync(string fullName, DbParameterWrap[] parameters, CancellationToken cancellationToken)
            => this.SelectTableFromStoredProcedureAsync(fullName, ToSqlParameters(parameters), cancellationToken);
        private async Task<DataTable> SelectTableFromStoredProcedureAsync(string fullName, SqlParameter[] parameters, CancellationToken cancellationToken)
        {
            DataSet ds = null;
            try
            {
                ds = await this.session.SqlAsync((session, ct) => session.StoredProcOpenAsync(fullName, parameters, ct), cancellationToken);
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

        public Task ExecStoredProcedureAsync(string fullName, DbParameterWrap[] parameters, CancellationToken cancellationToken)
            => this.ExecStoredProcedureAsync(fullName, ToSqlParameters(parameters), cancellationToken);
        private async Task ExecStoredProcedureAsync(string fullName, SqlParameter[] parameters, CancellationToken cancellationToken)
        {
            try
            {
                var returnParameters = await this.session.SqlAsync((session, ct) => session.StoredProcExecAsync(fullName, parameters, ct), cancellationToken);
            }
            catch (OperationCanceledException)
            {
                throw;
            }
            catch (Exception ex)
            {
                ExceptionDispatchInfo.Capture(new RepositoryException(ex.Message, ex)).Throw();
            }
        }


        public Task<DataSet> SelectSetFromStoredProcedureAsync(string fullName, DbParameterWrap[] parameters, CancellationToken cancellationToken)
            => this.SelectSetFromStoredProcedureAsync(fullName, ToSqlParameters(parameters), cancellationToken);
        private async Task<DataSet> SelectSetFromStoredProcedureAsync(string fullName, SqlParameter[] parameters, CancellationToken cancellationToken)
        {
            DataSet ds = null;
            try
            {
                ds = await this.session.SqlAsync((session, ct) => session.StoredProcOpenAsync(fullName, parameters, ct), cancellationToken);
            }
            catch (OperationCanceledException)
            {
                throw;
            }
            catch (Exception ex)
            {
                ExceptionDispatchInfo.Capture(new RepositoryException(ex.Message, ex)).Throw();
            }

            return ds;
        }




        public new abstract class SchemaBase : DbObjectWithSchemasBase.SchemaBase
        {
            public SchemaBase(Session session, StoredProceduresBase storedProcedures) : base(session)
            {
                this.StoredProcedures = storedProcedures;
            }

            public StoredProceduresBase StoredProcedures { get; private set; }
        }




        // We should use these return objects for stored procedures if there are any output parameters in them
        // This is because that async methods cannot have ref, in or out parameters:
        // https://social.msdn.microsoft.com/Forums/en-US/d2f48a52-e35a-4948-844d-828a1a6deb74/why-async-methods-cannot-have-ref-or-out-parameters
        // But we should strive to use async methods, so with the output parameters we use workaround


        /// <summary>
        /// Use this method inside of mapped stored procedure async method, where both result values and output parameters are going to be used
        /// </summary>
        /// <typeparam name="TValue"></typeparam>
        /// <typeparam name="TOutputParameterValues"></typeparam>
        /// <param name="value"></param>
        /// <param name="outputParameterValues"></param>
        /// <returns></returns>
        protected static SpSelectResult<TValue, TOutputParameterValues> SelectResult<TValue, TOutputParameterValues>(TValue value, TOutputParameterValues outputParameterValues)
            => new SpSelectResult<TValue, TOutputParameterValues>
            {
                Value = value,
                OutputParameterValues = outputParameterValues
            };


        /// <summary>
        /// Use this method inside of mapped stored procedure async method, where only output parameters are going to be used
        /// </summary>
        /// <typeparam name="TOutputParameterValues"></typeparam>
        /// <param name="outputParameterValues"></param>
        /// <returns></returns>
        protected static SpExecResult<TOutputParameterValues> ExecResult<TOutputParameterValues>(TOutputParameterValues outputParameterValues)
            => new SpExecResult<TOutputParameterValues> { OutputParameterValues = outputParameterValues };



        public class SpSelectResult<TValue, TOutputParameterValues>
        {
            public TValue Value { get; set; }
            public TOutputParameterValues OutputParameterValues { get; set; }
        }

        public class SpExecResult<TOutputParameterValues>
        {
            public TOutputParameterValues OutputParameterValues { get; set; }
        }
    }
}
