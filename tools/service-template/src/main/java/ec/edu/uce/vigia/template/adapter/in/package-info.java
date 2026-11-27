/**
 * Driving adapters (REST controllers, Kafka and RabbitMQ consumers). They translate input into
 * commands and never call repositories directly (DP-10).
 */
package ec.edu.uce.vigia.template.adapter.in;
