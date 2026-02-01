using Core.Db;

namespace Core.Repositories.Infrastructure
{
    public class DbObjects : Base.DbObjectBase
    {
        public DbObjects(Session session) : base(session)
        {
            this.TableValuedFunctions = new TableValuedFunctions(session);
            this.StoredProcedures = new StoredProcedures(session);
            this.ScalarValuedFunctions = new ScalarValuedFunctions(session);
        }

        /// <summary>
        /// Table-valued functions
        /// </summary>
        public TableValuedFunctions TableValuedFunctions { get; private set; }


        /// <summary>
        /// Stored procedures
        /// </summary>
        public StoredProcedures StoredProcedures { get; private set; }


        /// <summary>
        /// Scalar-valued functions
        /// </summary>
        public ScalarValuedFunctions ScalarValuedFunctions { get; private set; }
    }
}
