

namespace Core.Repositories.Infrastructure
{
    public abstract partial class RepositoryBase<TEntity> : Base.RepositoryTypedBase<RepositoryContainer>
    {
        /// <summary>
        /// Point to use database objects, which repeat their declarations in the database
        /// </summary>
        protected DbObjects DbObjects { get; private set; }

        protected override void InitOtherObjects()
        {
            this.DbObjects = new DbObjects(this.Session);
        }
    }
}
