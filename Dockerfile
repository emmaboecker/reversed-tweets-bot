FROM eclipse-temurin:17 as builder

COPY . .

RUN chmod +x gradlew && ./gradlew --no-daemon installDist

FROM eclipse-temurin:17

WORKDIR /user/app

COPY --from=builder build/install/reversetweets ./

ENTRYPOINT ["/user/app/bin/reversetweets"]