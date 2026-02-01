using Core.Repositories;
using Microsoft.AspNetCore.Authorization;
using Microsoft.AspNetCore.Mvc;

namespace PayBank.Controllers
{
    public class OperationController : Controller
    {
        private static OperationsRepository operationsRepository = new OperationsRepository();
        [Authorize(Policy = "AdminOnly")]
        [HttpGet]
        public async Task<IActionResult> Index()
        {
            var result = await operationsRepository.GetOperations();
            return View(result.ReturnObject);
        }
    }
}
