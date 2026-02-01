namespace Core.Repositories.Infrastructure.Base
{
    public abstract partial class RepositoryTypedBase<TRepositoryContainer> : RepositoryBase
        where TRepositoryContainer : RepositoryContainerBase
    {
        /// <summary>
        /// Point to use other repositories
        /// </summary>
        protected new TRepositoryContainer Repositories => (TRepositoryContainer)base.Repositories;
    }
}
