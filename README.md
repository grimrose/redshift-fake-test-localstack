# redshift-fake-driver and localstack

sample for [opt-tech/redshift-fake-driver] and [localstack/localstack]

## Requirements

* docker
* docker-compose
* sbt

## Usage

build sbt docker image

```sh
docker-compose build
```

and run sbt test

```sh
docker-compose run --rm app
```

## License

MIT

[localstack/localstack]: https://github.com/localstack/localstack
[opt-tech/redshift-fake-driver]: https://github.com/opt-tech/redshift-fake-driver
