using Microsoft.Extensions.Options;
using Minio.DataModel.Args;
using Minio;
using MongoDB.Driver;
using System.Threading;
using System.IO;
using HrWebRecruitment.Models.Config;

namespace HrWebRecruitment.Services
{
    public class MinioService(ILogger<MinioService> logger, IOptions<MinioConfigModel> minioConfig)
    {
        private const string BucketName = "cv-bucket";
        IMinioClient _minioClient = null;

        public async Task<bool> CheckIfExistsDoc(string bucketName, string objectPath, CancellationToken cancellationToken = default)
        {
            try
            {
                if (_minioClient == null)
                    ConnectMinioClient();

                // Check if object already exists
                await _minioClient!.StatObjectAsync(new StatObjectArgs()
                    .WithBucket(bucketName)
                    .WithObject(objectPath), cancellationToken);

                logger.LogDebug("Already exists in minioConfig: {BucketName}/{ObjectPath}. Skipping upload.", bucketName, objectPath);
                return true;
            }
            catch (Minio.Exceptions.ObjectNotFoundException exception) // Object does not exist, proceed to upload
            {
                logger.LogDebug(exception, "Not found in Minio, creating new document {BucketName}/{ObjectPath}", bucketName, objectPath);
                return false;
            }
        }

        public async Task<string> UploadCV(IFormFile file)
        {
            try
            {
                if (_minioClient == null)
                    ConnectMinioClient();

                // Ensure bucket exists
                bool found = await _minioClient.BucketExistsAsync(new BucketExistsArgs().WithBucket(BucketName));
                if (!found)
                {
                    await _minioClient.MakeBucketAsync(new MakeBucketArgs().WithBucket(BucketName));

                }

                var fileName = Guid.NewGuid() + Path.GetExtension(file.FileName);
                using var stream = file.OpenReadStream();
                var uploadResponse = await _minioClient.PutObjectAsync(new PutObjectArgs()
                    .WithBucket(BucketName)
                    .WithObject(fileName)
                    .WithStreamData(stream)
                    .WithObjectSize(file.Length)
                    .WithContentType(file.ContentType));

                string linkToCv = $"http://{minioConfig.Value.Endpoint}/{BucketName}/{fileName}";
                return linkToCv;
            }
            catch (Exception ex)
            {
                logger.LogError(ex.Message);
                return string.Empty;
            }
        }

        private void ConnectMinioClient()
        {
            _minioClient = new MinioClient()
                                   .WithEndpoint(minioConfig.Value.Endpoint)
                                   .WithCredentials(minioConfig.Value.AccessKey, minioConfig.Value.SecretKey)
                                   .WithSSL(minioConfig.Value.UseSSL)
                                   .Build();
        }
    }
}
