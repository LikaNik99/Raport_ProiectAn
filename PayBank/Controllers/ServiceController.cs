using Core.Repositories;
using Microsoft.AspNetCore.Authorization;
using Microsoft.AspNetCore.Mvc;
using Microsoft.AspNetCore.Mvc.Rendering;
using PayBank.Models;

namespace Pay.Net.Controllers
{
    [Authorize(Policy = "AdminOnly")]
    public class ServiceController : Controller
    {
        private readonly IWebHostEnvironment environment;
        private static ServicesRepository servicesRepository = new ServicesRepository();
        private static CategoriesRepository categoryRepository = new CategoriesRepository();


        public ServiceController(IWebHostEnvironment environment)
        {
            this.environment = environment;
        }
        [HttpGet]
        public async Task<IActionResult> Index()
        {
            var services = await servicesRepository.GetServices();
            return View(services.ReturnObject);
        }
        [HttpPost]
        public async Task<IActionResult> Index (string Search)
        {
            var result = await servicesRepository.GetServices();
            if(result.ResultCode != Core.Model.Common.PPResponseCode.SUCCESS)
            {
                return View();
            }

            var services = result.ReturnObject;
            if (!String.IsNullOrEmpty(Search))
            {
                services = services!.Where(s => s.Name.ToLower().Contains(Search.ToLower()) || s.CategoryName.ToLower().Contains(Search.ToLower()) || 
                s.Id.ToString().Contains(Search)).ToList();
            }
            return View(services);
        }

        [HttpGet]
        public async Task<IActionResult> Create()
        {
            var categories = await categoryRepository.GetCategories();
            ViewBag.Category = new SelectList(categories.ReturnObject, "Id","Name");
            return View();
        }
        [HttpPost]
        public async Task<IActionResult> Create(CreateServiceViewModel vm)
        {
            string imageFailName;
            if(vm.ImageFile == null)
            {
                ModelState.Remove("ImageFile");
            }
            if(ModelState.IsValid)
            {
                
                if (vm.ImageFile != null)
                {
                    string newFileName = await SaveImageAsync(vm.ImageFile);
                    imageFailName = newFileName;
                }
                else
                {
                    imageFailName = "NoImage.jpg";
                }
                var result = await servicesRepository.AddService(vm.Name, vm.CategoryId, imageFailName);

                if(result.ResultCode == Core.Model.Common.PPResponseCode.SUCCESS)
                {
                    TempData["Message"] = "Serviciul a fost adaugat cu succes";
                    TempData["AlertType"] = "success";
                    return RedirectToAction("Create", "Service");
                }
                else
                {
                    TempData["Message"] = result.ResultCode.ToString();
                    TempData["AlertType"] = "danger";
                    return RedirectToAction("Create", "Service");
                }
                
            }
            var categories = await categoryRepository.GetCategories();
            ViewBag.Category = new SelectList(categories.ReturnObject, "Id", "Name");
            return View(vm);
        }
        
        [HttpGet]
        public async Task<IActionResult> Edit(int id)
        {
            var services = await servicesRepository.GetService(id);
            if (services.ResultCode != Core.Model.Common.PPResponseCode.SUCCESS)
            {
                TempData["Message"] = "Serviciul nu a fost găsit.";
                TempData["AlertType"] = "danger";
                return RedirectToAction("Index", "Service");
            }
            var vm = new CreateServiceViewModel
            {
                Name = services.ReturnObject!.Name,
                CategoryId = services.ReturnObject.CategoryId,
                ImageFile = null,
            };
            var categories = await categoryRepository.GetCategories();
            ViewData["ServiceId"] = services.ReturnObject.Id;
            ViewData["ImageFileName"] = services.ReturnObject.ImageFileName;
            ViewBag.Category = new SelectList(categories.ReturnObject, "Id", "Name", services.ReturnObject.CategoryId);
            return View(vm);
        }
        [HttpPost]
        public async Task<IActionResult> Edit(int id, CreateServiceViewModel vm)
        {
            if (vm.ImageFile == null)
            {
                ModelState.Remove("ImageFile");
            }
            if (ModelState.IsValid)
            {
                var service = await servicesRepository.GetService(id);
                if (service.ResultCode != Core.Model.Common.PPResponseCode.SUCCESS)
                {
                    TempData["Message"] = "Serviciul nu a fost găsit.";
                    TempData["AlertType"] = "danger";
                    return RedirectToAction("Index", "Service");
                }
                if (vm.ImageFile != null)
                {
                    string newFileName = await SaveImageAsync(vm.ImageFile);
                    DeleteImage(service.ReturnObject!.ImageFileName);
                    service.ReturnObject.ImageFileName = newFileName;
                }

                var result = await servicesRepository.UpdateService(id, vm.Name, vm.CategoryId, service.ReturnObject!.ImageFileName);

                if(result.ResultCode == Core.Model.Common.PPResponseCode.SUCCESS)
                {
                    TempData["Message"] = "Serviciul a fost actualizat cu succes.";
                    TempData["AlertType"] = "success";
                    return RedirectToAction("Index", "Service");
                }
                else
                {
                    TempData["Message"] = result.ResultCode.ToString();
                    TempData["AlertType"] = "danger";
                    return RedirectToAction("Index", "Service");
                }
                
            }
            return View(vm);
        }
       
        [HttpDelete]
        public async Task<IActionResult> Delete(int id)
        {
            var result = await servicesRepository.DeleteService(id);

            if (result.ResultCode == Core.Model.Common.PPResponseCode.NOT_FOUND)
            {
                TempData["Message"] = "Serviciul nu a fost găsit.";
                TempData["AlertType"] = "danger";
                return RedirectToAction("Index", "Service");
            }
            else if(result.ResultCode == Core.Model.Common.PPResponseCode.SUCCESS)
            {
                DeleteImage(result.ReturnObject!);
                TempData["Message"] = "Serviciul a fost sters cu succes.";
                TempData["AlertType"] = "success";
                return Ok();
            }
            else
            {
                TempData["Message"] = result.ResultCode.ToString();
                TempData["AlertType"] = "danger";
                return RedirectToAction("Index", "Service");
            }
        }

        private async Task<string> SaveImageAsync(IFormFile imageFile)
        {
            Guid path = Guid.NewGuid();
            string newFileName = path.ToString() + Path.GetExtension(imageFile.FileName);
            string imageFullPath = Path.Combine(environment.WebRootPath, "SImg", newFileName);
            string directory = Path.GetDirectoryName(imageFullPath);
            if (!Directory.Exists(directory))
            {
                Directory.CreateDirectory(directory);
            }

            using (var stream = new FileStream(imageFullPath, FileMode.Create))
            {
                await imageFile.CopyToAsync(stream);
            }
            return newFileName;
        }

        private void DeleteImage(string fileName)
        {
            if (!string.IsNullOrEmpty(fileName))
            {
                if (fileName != "NoImage.jpg")
                {
                    string imagePath = Path.Combine(environment.WebRootPath, "SImg", fileName);
                    if (System.IO.File.Exists(imagePath))
                    {
                        System.IO.File.Delete(imagePath);
                    }
                }
            }
        }
    }
}
