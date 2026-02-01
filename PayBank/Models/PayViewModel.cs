using System.ComponentModel.DataAnnotations;
using Core.DBModel;

namespace PayBank.Models
{
    public class PayViewModel
    {
        public int TranId { get; set; }
        [Required(ErrorMessage = "Suma este obligatorie.")]
        [Range(0.01, double.MaxValue, ErrorMessage = "Introduceți o sumă validă mai mare ca 0.")]
        [DisplayFormat(DataFormatString = "{0:0.00}", ApplyFormatInEditMode = true)]
        public decimal Amount { get; set; }
        [Required(ErrorMessage = "Numărul de cont este obligatoriu.")]
        [Range(1, int.MaxValue, ErrorMessage = "Introduceți un număr de cont valid.")]
        public int ProviderId { get; set; }
        public EntityService EntityService { get; set; }
    }
}
