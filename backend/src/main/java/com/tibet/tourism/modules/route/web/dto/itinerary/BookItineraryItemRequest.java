package com.tibet.tourism.modules.route.web.dto.itinerary;
import com.tibet.tourism.modules.route.domain.Itinerary;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public class BookItineraryItemRequest {

    @Min(value = 1, message = "出行人数不能少于1人")
    @Max(value = 20, message = "出行人数不能超过20人")
    private Integer travelers;

    @Size(max = 64, message = "入住人姓名不能超过64个字符")
    private String guestName;

    @Size(max = 32, message = "手机号不能超过32个字符")
    @Pattern(regexp = "^$|^[0-9+\\-\\s()]{6,32}$", message = "手机号格式不合法")
    private String phone;

    @Size(max = 500, message = "备注不能超过500个字符")
    private String note;

    public Integer getTravelers() { return travelers; }
    public void setTravelers(Integer travelers) { this.travelers = travelers; }

    public String getGuestName() { return guestName; }
    public void setGuestName(String guestName) { this.guestName = guestName; }

    public String getPhone() { return phone; }
    public void setPhone(String phone) { this.phone = phone; }

    public String getNote() { return note; }
    public void setNote(String note) { this.note = note; }
}
