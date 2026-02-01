using Core.Repositories;
using Microsoft.AspNetCore.Authorization;
using Microsoft.AspNetCore.Mvc;
using Microsoft.AspNetCore.SignalR;
using PayBank.Infrastructure;
using PayBank.Models;
using System.Diagnostics;
using System.Security.Claims;

namespace PayBank.Controllers
{
    public class HomeController : Controller
    {
        private static ServicesRepository serviceRepository = new ServicesRepository();
        public OperationsRepository operation = new OperationsRepository();
        private readonly IHubContext<OperationHub> _hub = null!;

        public HomeController(IHubContext<OperationHub> hub)
        {
            _hub = hub;
        }

        public IActionResult Index()
        {

            return View();
        }
        [HttpGet]
        public async Task<IActionResult> Services()
        {

            var result = await serviceRepository.GetServices();
            if (result.ResultCode == Core.Model.Common.PPResponseCode.SUCCESS)
            {
                var viewModel = result.ReturnObject!.GroupBy(b => new { b.CategoryId, b.CategoryName })
                    .Select(group => new CategoryServiceViewModel
                    {
                        CategoryId = group.Key.CategoryId,
                        CategoryName = group.Key.CategoryName,
                        Services = group.ToList()
                    });
                TempData["display"] = "none";
                return View(viewModel);
            }

            return View();
        }
        [HttpPost]
        public async Task<IActionResult> Services(string search)
        {
            var result = await serviceRepository.GetServices();
            var services = result.ReturnObject;
            if (!String.IsNullOrEmpty(search))
            {
                services = services!.Where(c => c.Name.ToLower().Contains(search.ToLower())).ToList();
            }

            var viewModel = services!.GroupBy(b => new { b.CategoryId, b.CategoryName })
                   .Select(group => new CategoryServiceViewModel
                   {
                       CategoryId = group.Key.CategoryId,
                       CategoryName = group.Key.CategoryName,
                       Services = group.ToList()
                   });
            TempData["display"] = "active";
            return View(viewModel);

        }

        [Authorize(Policy = "UserOnly")]
        [HttpGet]
        public async Task<IActionResult> Pay(int id)
        {
            var result = await serviceRepository.GetService(id);

            if (result.IsOK)
            {
                var service = new PayViewModel();
                service.EntityService = result.ReturnObject!;
                return View(service);
            }
            else
            {
                return RedirectToAction("Services", "Home");
            }
        }

        [Authorize(Policy = "UserOnly")]
        [HttpPost]
        public async Task<IActionResult> Pay(PayViewModel vm)
        {
            var userId = User.FindFirstValue("UserId");

            var amount = (long)(vm.Amount * 100);
            var result = await serviceRepository.PayService(int.Parse(userId), amount, vm.EntityService.Id, vm.ProviderId);

            if (result.IsOK)
            {
                var service = await serviceRepository.GetService(vm.EntityService.Id);
                var model = new PayViewModel();
                model.EntityService = service.ReturnObject!;
                model.Amount = vm.Amount;
                model.TranId = result.ReturnObject;
                model.ProviderId = vm.ProviderId;
                var op = await operation.GetOperation(result.ReturnObject);
                await _hub.Clients.All.SendAsync("NewOperation", op.ReturnObject);
                TempData["Message"] = "Serviciul a fost platit cu succes";
                TempData["AlertType"] = "success";
                return View(model);
            }
            else
            {
                TempData["Message"] = "Nu aveți destui bani in cont";
                TempData["AlertType"] = "danger";
                return RedirectToAction("Pay", "Service");
            }
        }
        [Authorize(Policy = "AdminOnly")]
        public IActionResult AdminPanel()
        {

            return View();
        }

        public IActionResult Error404()
        {
            ViewData["Message"] = "Your contact page.";

            return View();
        }

        public IActionResult Error()
        {
            return View(new ErrorViewModel { RequestId = Activity.Current?.Id ?? HttpContext.TraceIdentifier });
        }
    }
}
