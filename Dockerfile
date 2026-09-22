# Estágio 1: Build da aplicação
FROM maven:3.9.6-eclipse-temurin-21 AS build
WORKDIR /app

# 1. Copia apenas o pom.xml primeiro
COPY pom.xml .

# 2. Baixa as dependências para o cache do Docker (se o pom.xml não mudar, essa etapa é pulada nas próximas vezes)
RUN mvn dependency:go-offline

# 3. Copia o código fonte
COPY src ./src

# 4. Compila o projeto
RUN mvn clean package -Dmaven.test.skip=true

# Estágio 2: Imagem final leve para rodar a aplicação
FROM eclipse-temurin:21-jre
WORKDIR /app
COPY --from=build /app/target/*.jar app.jar

# Expõe a porta 8080
EXPOSE 8080

# Define o fuso horário para São Paulo
ENV TZ=America/Sao_Paulo

# Comando para iniciar a aplicação
ENTRYPOINT ["java", "-jar", "app.jar"]