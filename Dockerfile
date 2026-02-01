FROM mcr.microsoft.com/dotnet/aspnet:7.0 AS base
WORKDIR /app
EXPOSE 80
EXPOSE 443

FROM mcr.microsoft.com/dotnet/sdk:7.0 AS build
WORKDIR /src
COPY ["PayBank/PayBank.csproj", "PayBank/"]
COPY ["Repositories/Core.Repositories.csproj", "Repositories/"]
COPY ["Core.DBModel/Core.DBModel.csproj", "Core.DBModel/"]
COPY ["Core.DBUtils/Core.DBUtils.csproj", "Core.DBUtils/"]
COPY ["Core.Model/Core.Model.csproj", "Core.Model/"]
COPY ["Core.Utils/Core.Utils.csproj", "Core.Utils/"]
RUN dotnet restore "PayBank/PayBank.csproj"
COPY . .
WORKDIR "/src/PayBank"
RUN dotnet build "PayBank.csproj" -c Release -o /app/build

FROM build AS publish
RUN dotnet publish "PayBank.csproj" -c Release -o /app/publish /p:UseAppHost=false

FROM base AS final
WORKDIR /app
COPY --from=publish /app/publish .
ENTRYPOINT ["dotnet", "PayBank.dll"]
