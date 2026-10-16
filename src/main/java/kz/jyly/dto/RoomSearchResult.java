package kz.jyly.dto;

import kz.jyly.entity.Room;

import java.util.List;

/**
 * @param rooms           көрсетілетін бөлмелер
 * @param busyCount       сүзгіге сай, бірақ таңдалған уақытта бос емес бөлмелер саны
 * @param availabilityChecked уақыт аралығы бойынша бос-бостығы тексерілді ме
 * @param error           іздеу параметрлеріндегі қате (болмаса null)
 */
public record RoomSearchResult(List<Room> rooms, int busyCount, boolean availabilityChecked, String error) {
}
