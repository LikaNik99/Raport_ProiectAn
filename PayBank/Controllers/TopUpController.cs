using Core.Repositories;
using Microsoft.AspNetCore.Authorization;
using Microsoft.AspNetCore.Mvc;
using PayBank.Models;
using System.Security.Claims;

namespace PayBank.Controllers
{
    public class TopUpController : Controller
    {
        private static ServicesRepository serviceRepository = new ServicesRepository();
        public OperationsRepository operation = new OperationsRepository();

        [Authorize(Policy = "UserOnly")]
        [HttpGet]
        public IActionResult TopUp()
        {
            return View(new TopUpViewModel());
        }

        [Authorize(Policy = "UserOnly")]
        [HttpPost]
        public async Task<IActionResult> TopUp(TopUpViewModel vm)
        {
            var userId = User.FindFirstValue("UserId");

            var amount = (long)(vm.Amount * 100);
            var result = await serviceRepository.TopUpPay(int.Parse(userId), amount);

            if (result.IsOK)
            {
                var model = new TopUpViewModel();
                model.Amount = vm.Amount;
                model.TranId = result.ReturnObject;
                await operation.GetOperation(result.ReturnObject);
                TempData["Message"] = "Alimentarea a avut loc cu succes";
                TempData["AlertType"] = "success";
                return View(model);
            }
            else
            {
                TempData["Message"] = "Nu aveți destui bani in cont";
                TempData["AlertType"] = "danger";
                return RedirectToAction("TopUp", "TopUp");
            }
        }
    }
}
