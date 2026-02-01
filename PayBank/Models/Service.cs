namespace PayBank.Models
{
    public class Service
    {
        public int ServiceId { get; set; }
        public string Name { get; set; }
        public int CategoryId { get; set; }
        public string ImageFileName { get; set; }
        public Category Category { get; set; }

    }

    public class CreateServiceViewModel
    {
        public string Name { get; set; } = null!;
        public int CategoryId { get; set; }
        public IFormFile? ImageFile { get; set; }

    }
    public class ServiceViewModel
    {
        public string ServiceName { get; set; }
        public string ImageFileName { get; set; }
    }
}
