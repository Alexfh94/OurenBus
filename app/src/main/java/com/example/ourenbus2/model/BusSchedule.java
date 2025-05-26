package com.example.ourenbus2.model;

import java.util.List;

/**
 * Clase que representa un horario de autobús para una línea específica.
 */
public class BusSchedule {
    private int id;
    private int busLineId;
    private String dayType;  // WEEKDAY, SATURDAY, SUNDAY
    private List<ScheduleTime> times;

    /**
     * Constructor por defecto.
     */
    public BusSchedule() {
    }

    /**
     * Constructor con parámetros.
     *
     * @param id        Identificador único del horario
     * @param busLineId Identificador de la línea de autobús
     * @param dayType   Tipo de día (WEEKDAY, SATURDAY, SUNDAY)
     * @param times     Lista de horarios por parada
     */
    public BusSchedule(int id, int busLineId, String dayType, List<ScheduleTime> times) {
        this.id = id;
        this.busLineId = busLineId;
        this.dayType = dayType;
        this.times = times;
    }

    /**
     * Obtiene el identificador único del horario.
     *
     * @return Identificador del horario
     */
    public int getId() {
        return id;
    }

    /**
     * Establece el identificador único del horario.
     *
     * @param id Identificador del horario
     */
    public void setId(int id) {
        this.id = id;
    }

    /**
     * Obtiene el identificador de la línea de autobús.
     *
     * @return Identificador de la línea
     */
    public int getBusLineId() {
        return busLineId;
    }

    /**
     * Establece el identificador de la línea de autobús.
     *
     * @param busLineId Identificador de la línea
     */
    public void setBusLineId(int busLineId) {
        this.busLineId = busLineId;
    }

    /**
     * Obtiene el tipo de día del horario.
     *
     * @return Tipo de día (WEEKDAY, SATURDAY, SUNDAY)
     */
    public String getDayType() {
        return dayType;
    }

    /**
     * Establece el tipo de día del horario.
     *
     * @param dayType Tipo de día (WEEKDAY, SATURDAY, SUNDAY)
     */
    public void setDayType(String dayType) {
        this.dayType = dayType;
    }

    /**
     * Obtiene la lista de horarios por parada.
     *
     * @return Lista de horarios
     */
    public List<ScheduleTime> getTimes() {
        return times;
    }

    /**
     * Establece la lista de horarios por parada.
     *
     * @param times Lista de horarios
     */
    public void setTimes(List<ScheduleTime> times) {
        this.times = times;
    }

    /**
     * Clase interna que representa un horario específico para una parada.
     */
    public static class ScheduleTime {
        private int busStopId;
        private String time;  // Formato HH:MM

        /**
         * Constructor por defecto.
         */
        public ScheduleTime() {
        }

        /**
         * Constructor con parámetros.
         *
         * @param busStopId Identificador de la parada
         * @param time      Hora en formato HH:MM
         */
        public ScheduleTime(int busStopId, String time) {
            this.busStopId = busStopId;
            this.time = time;
        }

        /**
         * Obtiene el identificador de la parada.
         *
         * @return Identificador de la parada
         */
        public int getBusStopId() {
            return busStopId;
        }

        /**
         * Establece el identificador de la parada.
         *
         * @param busStopId Identificador de la parada
         */
        public void setBusStopId(int busStopId) {
            this.busStopId = busStopId;
        }

        /**
         * Obtiene la hora del horario.
         *
         * @return Hora en formato HH:MM
         */
        public String getTime() {
            return time;
        }

        /**
         * Establece la hora del horario.
         *
         * @param time Hora en formato HH:MM
         */
        public void setTime(String time) {
            this.time = time;
        }
    }
} 