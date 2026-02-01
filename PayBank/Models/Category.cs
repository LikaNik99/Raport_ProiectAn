using Core.DBModel;

namespace PayBank.Models
{
    public class Category
    {
        public int CategoryId { get; set; }
        public string Name { get; set; }
        public ICollection<Service> Services { get; set; }
    }
    public class CreateCategoryViewModel
    {
        public string Name { get; set; }
    }
    public class CategoryServiceViewModel
    {
        public int CategoryId { get; set; }
        public string CategoryName { get; set; }
        public List<EntityService> Services { get; set; }
    }
}
