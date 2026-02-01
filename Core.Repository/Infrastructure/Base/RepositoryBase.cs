using Core.Db;

namespace Core.Repositories.Infrastructure.Base
{
    public abstract partial class RepositoryBase : ICreateRepository
    {
        protected Session Session { get; private set; }

        void ICreateRepository.SetInitObjects(Session session, RepositoryContainerBase repositories)
        {
            this.Session = session;
            this.Repositories = repositories;
            this.InitOtherObjects();
        }


        /// <summary>
        /// Init other objects after invoking <see cref="ICreateRepository.SetInitObjects(Session, RepositoryContainerBase)"/>
        /// </summary>
        protected virtual void InitOtherObjects() { }


        /// <summary>
        /// Point to use other repositories
        /// </summary>
        protected RepositoryContainerBase Repositories { get; private set; }


        /// <summary>
        /// Object for include additional TotalCount information relating to the return list of items with paging functionality
        /// </summary>
        /// <typeparam name="TResultItem"></typeparam>
		public class DbResultList<TResultItem>
        {
            public int TotalCount { get; set; }
            public IList<TResultItem> ResultItems { get; set; }
        }
    }


    /// <summary>
    /// Interface to explicitely set session to the repository
    /// </summary>
    public interface ICreateRepository
    {
        void SetInitObjects(Session session, RepositoryContainerBase repositories);
    }
}
