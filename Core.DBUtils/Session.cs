using Core.DBUtils.Db;

namespace Core.Db
{
    public class Session
    {
        readonly string _connectionString;
        int _commandTimeout;

        public Session(string connectionString, int commandTimeout = 30)
        {
            _connectionString = string.IsNullOrEmpty(connectionString) ? string.Empty : connectionString;
            _commandTimeout = commandTimeout;
        }

        public delegate T SqlMethod<T>(Mssql session);
        public delegate T SqlMethodWithCancellation<T>(Mssql session, CancellationToken cancellationToken);

        public virtual T Sql<T>(SqlMethod<T> method)
        {
            using (Mssql session = new Mssql(_connectionString))
            {
                session.CommandTimeout = _commandTimeout;
                session.Open();
                return method(session);
            }
        }

        public virtual async Task<T> SqlAsync<T>(SqlMethodWithCancellation<Task<T>> method, CancellationToken cancellationToken)
        {
            using (var session = new Mssql(this._connectionString))
            {
                session.CommandTimeout = this._commandTimeout;
                await session.OpenAsync(cancellationToken);
                return await method(session, cancellationToken);
            }
        }
    }
}
