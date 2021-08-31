import axios from 'axios';
import { ServiceStatus } from '@/model/ServiceStatus';
import { ServiceStatusDto } from '@/dto/ServiceStatusDto';

export class StatusesService {
  url = '/api/v1/service';

  public async getAll(): Promise<ServiceStatus[]> {
    const response = await axios.get(this.url);
    return response.data.map((x: ServiceStatusDto) => {
      return {
        url: x.url,
        name: x.name,
        createdAt: new Date(x.created_at),
        status: x.status
      };
    });
  }

  public async delete(name: string): Promise<boolean> {
    const response = await axios.delete(`${this.url}/${name}`, {
      responseType: 'text'
    });

    return response.status === 204;
  }

  public async add(name: string, url: string): Promise<boolean> {
    const response = await axios.post(this.url, {
      name: name,
      url: url
    }, {
      responseType: 'text'
    });

    return response.status === 201;
  }
}
