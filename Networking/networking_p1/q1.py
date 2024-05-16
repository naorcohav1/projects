import random
import queue
import time
from threading import Thread, Event

request_sent = 0 #total requests
failed_requests = 0  # Counter for failed requests
DEADLINE= 0.12
RATE= 5
DURATION= 30

class FIFOQueue:
    def __init__(self, max_size):
        self.queue = queue.Queue(maxsize=max_size)

    def add_request(self, request):
        try:
            self.queue.put(request, block=False)
        except queue.Full:
            print("Queue is full. Request dropped.")

    def get_next_request(self):
        try:
            return self.queue.get(block=False)
        except queue.Empty:
            return None

    def is_empty(self):
        return self.queue.empty()

class LIFOQueue:
    def __init__(self, max_size):
        self.queue = queue.LifoQueue(maxsize=max_size)

    def add_request(self, request):
        try:
            self.queue.put(request, block=False)
        except queue.Full:
            print("Queue is full. Request dropped.")

    def get_next_request(self):
        try:
            return self.queue.get(block=False)
        except queue.Empty:
            return None

    def is_empty(self):
        return self.queue.empty()

class PriorityQueue:
    def __init__(self, max_size):
        self.queue = queue.PriorityQueue(maxsize=max_size)

    def add_request(self, request):
        try:
            self.queue.put(request, block=False)
        except queue.Full:
            print("Queue is full. Request dropped.")

    def get_next_request(self):
        try:
            return self.queue.get(block=False)
        except queue.Empty:
            return None

    def is_empty(self):
        return self.queue.empty()

class Request:
    def __init__(self, id):
        self.id = id
        self.complexity = random.uniform(0.001,0.1)  # Complexity based on internet time avergae for DNS requests.
        self.deadline = DEADLINE  # suppose to be from 1 sec to 10 but it cause no failures so we ajusted it to somthing much smaller
        self.status = None  # Status will be set after processing
        self.time=time.time()
        self.priority = random.randint(1, 10)  # Random priority between 1 and 10 (only works for priority queue)

    def __lt__(self, other):
        return self.priority < other.priority

    def __eq__(self, other):
        return self.priority == other.priority

class Server(Thread):
    def __init__(self, queue):
        super().__init__()
        self.queue = queue #chose from 1-3 options
        self.processing_times_counter = 0  #total time of requests
        self.request_counter = 0 #total requests
        self.start()

    def run(self):
        global failed_requests
        while True:
            if not self.queue.is_empty():
                request = self.queue.get_next_request()
                start_time = request.time  # Record start time
                time.sleep(request.complexity)  # Processing time depends on query complexity
                end_time = time.time()  # Record end time
                processing_time = end_time - start_time  # Calculate processing time

                self.processing_times_counter=self.processing_times_counter+processing_time  
                self.request_counter+=1
                if processing_time <= request.deadline:
                    request.status = "Successful"
                else:
                    request.status = "Failed"
                    failed_requests += 1  # Increment failed requests counter
                print(f"Search query {request.id} processed: {request.status} - Processing Time: {processing_time:.2f}s")

    def get_average_response_time(self):
        if self.processing_times_counter:
            return self.processing_times_counter/self.request_counter
        else:
            return 0.0

class RequestGenerator(Thread):
    def __init__(self, queue, arrival_rate, stop_event):
        super().__init__()
        self.queue = queue
        self.id_counter = 0
        self.arrival_rate = arrival_rate
        self.stop_event = stop_event  # Event to signal when to stop
        self.start()

    def run(self):
        global request_sent  # Declare as global
        global failed_requests  # Declare as global

        while not self.stop_event.is_set():  # Continue generating requests until stop event is set
            self.id_counter += 1
            request = Request(self.id_counter)
            try:
                self.queue.add_request(request)
                print(f"New search query (ID: {request.id}) generated")
            except queue.Full:
                failed_requests+=1
            print(f"New search query (ID: {request.id}) generated in {request.time}")
            request_sent+=1
            time.sleep(random.expovariate(self.arrival_rate))  # Simulate Poisson arrival process


def main():
    # Selecting Queue Mechanism and setting parameters
    print("Select Queue Mechanism:")
    print("1. FIFO")
    print("2. LIFO")
    print("3. Priority")
    choice = input("Enter your choice (1/2/3): ")



    max_queue_size = int(input("Enter maximum queue size: "))

    queue_map = {
        "1": FIFOQueue,
        "2": LIFOQueue,
        "3": PriorityQueue
    }

    selected_queue = queue_map.get(choice)
    if not selected_queue:
        print("Invalid choice. Exiting...")
        return

    queue_instance = selected_queue(max_queue_size)

    # Initialize server and request generator
    stop_event = Event()  # Event to signal when to stop the simulation
    server = Server(queue_instance)
    request_generator = RequestGenerator(queue_instance, arrival_rate=RATE, stop_event=stop_event)  # Adjust arrival rate as needed

    # Run simulation
    start_time = time.time()
    while time.time() - start_time < DURATION:
        time.sleep(1)

    # Set the stop event to signal the request generator to stop
    stop_event.set()

    # Print average response time
    avg_response_time = server.get_average_response_time()
    if failed_requests !=0  :
        suc_rate=((request_sent-failed_requests)/request_sent)*100
    else:
        suc_rate=100
    print("--------------------------------")
    print(f"While using {selected_queue} with the rate of {RATE} and deadline of {DEADLINE} seconds")
    print(f"Total requests: {request_sent} and the time of this experiment is {DURATION} seconds")
    print(f"The queue cap is {max_queue_size}")
    print(f"Average response time for the selected queue mechanism: {avg_response_time:.2f} seconds")
    print(f"Failed requests: {failed_requests}")
    print(f"Success rate: {suc_rate}%")
    print("--------------------------------")
    return 1

if __name__ == "__main__":
    main()
