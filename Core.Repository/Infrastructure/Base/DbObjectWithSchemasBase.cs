using System.Collections.Concurrent;
using System.Reflection;
using Core.Db;

namespace Core.Repositories.Infrastructure.Base
{
    public abstract class DbObjectWithSchemasBase : DbObjectBase
    {
        public DbObjectWithSchemasBase(Session session) : base(session)
        {
            this.InitSchemas(session);
        }


        private static ConcurrentDictionary<Type, List<PropertyInfo>> typePropertyListDist = new ConcurrentDictionary<Type, List<PropertyInfo>>();

        protected void InitSchemas(Session session)
        {
            var props = typePropertyListDist.GetOrAdd(this.GetType(), t =>
            {
                var propertiesFlat = new List<PropertyInfo>();
                var properties = this.GetType().GetProperties();

                var groups = properties.GroupBy(p => p.Name);
                foreach (var group in groups)
                {
                    if (group.Count() == 1)
                    {
                        var single = group.First();
                        propertiesFlat.Add(single);
                        continue;
                    }

                    // we presume that the first one will be from the most specific type of SchemaBase with corresponding to the current type
                    var first = group.First();
                    propertiesFlat.Add(first);
                }

                return propertiesFlat;
            });



            foreach (var property in props)
            {
                var propertyType = property.PropertyType;

                if (!propertyType.IsSubclassOf(typeof(SchemaBase)))
                    continue;

                var value = Activator.CreateInstance(propertyType, session, this);
                property.SetValue(this, value);
            }
        }


        public abstract class SchemaBase : DbObjectBase
        {
            protected SchemaBase(Session session) : base(session) { }
        }
    }
}
