using System.Collections.Concurrent;
using System.Configuration;
using Core.Db;
using Core.DBUtils;

namespace Core.Repositories.Infrastructure.Base
{
    public abstract class RepositoryContainerBase : IDisposable
    {
        private ConcurrentDictionary<Type, ICreateRepository> dict = new ConcurrentDictionary<Type, ICreateRepository>();

        protected TRepository GetRepository<TRepository>() where TRepository : ICreateRepository, new()
        {
            var repository = this.dict.GetOrAdd(typeof(TRepository), new TRepository());
            repository.SetInitObjects(this.session, this);
            return (TRepository)repository;
        }


        private Session session;
        public bool IsTransactioned { get; private set; }

        private TransactionedSession TransactionedSession => this.session as TransactionedSession;

        public RepositoryContainerBase(string connectionString, bool isTransactioned = false)
        {
            var doNotUseAsyncCommandStr = ConfigurationManager.AppSettings["doNotUseAsyncCommand"];
            bool doNotUseAsyncCommand = new[] { "true", "1" }.Contains(doNotUseAsyncCommandStr, StringComparer.OrdinalIgnoreCase);

            this.session = isTransactioned
                ? new TransactionedSession(connectionString, doNotUseAsyncCommand: doNotUseAsyncCommand)
                : new Session(connectionString, doNotUseAsyncCommand: doNotUseAsyncCommand);

            this.IsTransactioned = isTransactioned;
        }


        public void Dispose() => (this.session as TransactionedSession)?.Dispose();

        public void BeginTransaction() => BeginTransaction(TransactionIsolationLevel.ReadCommitted);
        public void BeginTransaction(TransactionIsolationLevel transactionIsolationLevel)
        {
            if (!this.IsTransactioned)
                throw new InvalidOperationException("Cannot used transactions if this object is not transactioned. Create this object with the 'isTransactioned' flag");

            this.TransactionedSession.BeginTransaction(transactionIsolationLevel);
        }

        public void CommitTransaction()
        {
            if (!this.IsTransactioned)
                throw new InvalidOperationException("Cannot used transactions if this object is not transactioned. Create this object with the 'isTransactioned' flag");

            this.TransactionedSession.CommitTransaction();
        }


        public void RollbackTransaction()
        {
            if (!this.IsTransactioned)
                throw new InvalidOperationException("Cannot used transactions if this object is not transactioned. Create this object with the 'isTransactioned' flag");

            this.TransactionedSession.RollbackTransaction();
        }



        protected static class ConnectionStrings
        {
            public static string GetBaseDB() => (ConfigurationHelper.GetConnectionString("BaseDB"));
        }
    }
}
