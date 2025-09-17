namespace passenger_microservice;

public class RideRequest
{
    public int Id { get; set; }
    public int PassengerId { get; set; }
    public string PickupLocation { get; set; }
    public string DropoffLocation { get; set; }
}
