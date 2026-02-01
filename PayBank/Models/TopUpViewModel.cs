using System.ComponentModel.DataAnnotations;

namespace PayBank.Models
{
    public class TopUpViewModel
    {
        [Required(ErrorMessage = "Numărul cardului este obligatoriu.")]
        [RegularExpression(@"^[4-5][0-9]{15}$", ErrorMessage = "Cardul trebuie să conțină 16 cifre și să înceapă cu 4 sau 5.")]
        public string Pan { get; set; } = null!;

        // CVC
        [Required(ErrorMessage = "CVC este obligatoriu.")]
        [RegularExpression(@"^[0-9]{3}$", ErrorMessage = "CVC trebuie să fie format din 3 cifre.")]
        public string Cvc { get; set; } = null!;

        // Expire (MM/YY)
        [Required(ErrorMessage = "Data expirării este obligatorie.")]
        public string Expire { get; set; } = null!;

        public int TranId { get; set; }

        [Required(ErrorMessage = "Suma este obligatorie.")]
        [Range(0.01, double.MaxValue, ErrorMessage = "Introduceți o sumă validă mai mare ca 0.")]
        [DisplayFormat(DataFormatString = "{0:0.00}", ApplyFormatInEditMode = true)]
        public decimal Amount { get; set; }
    }
}
